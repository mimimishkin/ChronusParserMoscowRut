package datasource.network.chronus.yourcity.youruniversity

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import model.common.parseRus
import kotlinx.coroutines.cancel
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DateTimeUnit.Companion.MINUTE
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import kotlinx.datetime.until
import kotlinx.serialization.json.Json
import library.logger.LogType
import library.logger.log
import model.chronus.EntryInfo
import model.chronus.Lesson
import model.chronus.LessonType
import model.chronus.Place.YOUR_PLACE
import model.chronus.Schedule
import model.chronus.asLessonType
import model.chronus.asString
import model.common.asDayOfWeek
import model.common.asLocalTime
import model.common.asMonth
import model.common.weekStartDay
import kotlin.String

suspend fun getLessons(client: HttpClient, json: Json, schedule: Schedule): List<Lesson>? {
	return try {
		supervisorScope {
			parseTimetable(client, schedule.url)
		}
	} catch (e: Exception) {
		log(LogType.NetworkClientError, e)
		return null
	}
}

private suspend fun parseTimetable(client: HttpClient, url: String, parseOther: Boolean = true): List<Lesson>? {
	val page = try {
		client.get(url).bodyAsText()
	} catch (e: Exception) {
		log(LogType.NetworkClientError, e)
		return null
	}

	return withContext(Dispatchers.Default) {
		val page = Ksoup.parse(page)
		val timetableList = page.getElementsByClass("nav-tabs").singleOrNull()
		if (timetableList == null)
			return@withContext emptyList()

		val otherLessons = if (parseOther) {
			timetableList.getElementsByTag("a").drop(1).map { it: Element ->
				async(Dispatchers.IO) {
					val res = parseTimetable(client, YOUR_PLACE.defaultUrl.dropLast(1) + it.attr("href"), false)
					if (res == null)
						cancel()
					res!!
				}
			}
		} else emptyList()

		val zone = TimeZone.of(YOUR_PLACE.city.timeZoneId)
		val today = Clock.System.todayIn(zone)
		val days = page.getElementsByClass("info-block_collapse")
		val lessons = days.flatMap { day ->
			val date = day.attribute("data-date")?.let { LocalDate.parseRus(it.value) }
				?: day.select(".info-block__header > span > span").text().parseRusShortDate(today)
				?: run {
					val dayOfWeek = day.select(".info-block__header > span").text().asDayOfWeek()!!
					val weekContainer = day.parents().firstOrNull { it.id().startsWith("week") }
					val period = 1 + (weekContainer?.siblingElements()?.size ?: 0)
					val week = weekContainer?.id()?.substringAfter('-')?.toInt() ?: 1
					val activeTimetableLink = timetableList.getElementsByClass("active").first()!!.child(0)
					val start = LocalDate.parse(activeTimetableLink.attr("href").substringAfter("start=").substringBefore('&'))

					val periodsBetween = start.until(today, DateTimeUnit.WEEK) / period
					val originalWeek = start.plus(periodsBetween * period + week - 1, DateTimeUnit.WEEK)
					originalWeek.weekStartDay().plus(dayOfWeek.ordinal, DateTimeUnit.DAY)
				}

			if (date.daysUntil(today) <= 7) { // оставляем расписание максимум на неделю назад (если предоставляется)
				day.getElementsByClass("timetable__list-timeslot").flatMap { slot ->
					val (timeElement, contentElement) = slot.children()

					val time = timeElement.text().substringAfter(", ")
					val (start, end) = time.trim().split(" — ").map { it.asLocalTime()!!.atDate(date) }

					contentElement.children().chunked(3).map { (title, about) ->
						val rawType = title.text()
						val isOnline = "Вебинар" in rawType
						val type = (if (isOnline) rawType.replace(" (Вебинар)", "") else rawType)
						val name = title.nextSibling()!!.toString().trim()

						val professors = about.getElementsByClass("icon-academic-cap").map {
							EntryInfo(it.attr("title"), YOUR_PLACE.defaultUrl.dropLast(1) + it.attr("href"))
						}.toSet()

						val locationHints = mutableListOf<String>()
						val locations = if (isOnline) {
							setOf(EntryInfo("Вебинар", null))
						} else {
							about.getElementsByClass("icon-location").map {
								val hint = it.attr("title")
								locationHints += hint

								val name = it.lastChild().toString().replace("Аудитория ", "").trim()
								val url = it.attribute("href")?.let { YOUR_PLACE.defaultUrl + it.value }
								EntryInfo(name, url)
							}.toSet()
						}

						val groups = about.getElementsByClass("icon-community").map {
							val name = it.lastChild().toString().trim()
							val url = it.attribute("href")?.let { YOUR_PLACE.defaultUrl + it.value } ?: url
							EntryInfo(name, url)
						}
						val (sub, ordinal) = groups.partition { "п/гр." in it.name }

						Lesson(
							name = name,
							type = type.let {
								fun LessonType.asShortString(): String? = when (this) {
									LessonType.Lecture -> "Лек."
									LessonType.Practice -> "Пр."
									LessonType.LabWork -> "Лаб."
									LessonType.Project -> "Проект"
									LessonType.Exam -> "Экз."
									LessonType.CourseCredit -> "Зач."
									LessonType.Consultation -> "Конс."
									is LessonType.Other -> asString()
								}

								if (it.contains(';') || it.contains(',')) {
									val types = it.split(';', ',').map { it.asLessonType() }
									LessonType.Other(types.joinToString(", ") { it.asShortString()!! })
								} else {
									it.asLessonType()
								}
							},
							startTime = start,
							durationInMinutes = start.toInstant(zone).until(end.toInstant(zone), MINUTE).toInt(),
							groups = ordinal.toSet(),
							subgroups = sub.map { it.name.substringAfter('.').toInt() }.toSet(),
							persons = professors,
							classrooms = locations,
							additionalInfo = locationHints.takeIf { it.isNotEmpty() }?.let {
								if (it.size == 1) {
									"Подсказка к аудитории: " + it[0]
								} else {
									"Подсказки к аудиториям:\n" + it.joinToString("\n")
								}
							}
						)
					}
				}
			} else {
				emptyList()
			}
		}

		lessons + otherLessons.awaitAll().flatten()
	}
}

/**
 * Даты вида 12 ноября, 31 Декабря...
 */
private fun String.parseRusShortDate(now: LocalDate): LocalDate? {
	return if (this.isNotEmpty()) { // ошибка буквально только в одном расписании
		val day = this.substringBefore(' ').toInt()
		val month = this.substringAfter(' ').asMonth()!!
		val year = if (month.ordinal - now.monthNumber > 7) now.year + 1 else now.year
		LocalDate(year, month, day)
	} else {
		null
	}
}
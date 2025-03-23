package datasource.network.chronus.yourcity.youruniversity

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import kotlinx.serialization.json.Json
import library.logger.LogType
import library.logger.log
import model.chronus.EntryInfo
import model.chronus.Lesson
import model.chronus.Place.YOUR_PLACE
import model.chronus.Schedule
import model.chronus.asLessonType
import model.common.asDayOfWeek
import model.common.asLocalTime
import model.common.asMonth
import model.common.weekStartDay
import kotlin.String

suspend fun getLessons(client: HttpClient, json: Json, schedule: Schedule): List<Lesson>? {
	return parseTimetable(client, schedule.url)
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
			supervisorScope {
				timetableList.getElementsByTag("a").drop(1).map { it: Element ->
					async {
						val res = parseTimetable(client, YOUR_PLACE.defaultUrl.dropLast(1) + it.attr("href"), false)
						if (res == null)
							cancel()
						res!!
					}
				}
			}
		} else emptyList()

		val zone = TimeZone.of(YOUR_PLACE.city.timeZoneId)
		val now = Clock.System.now().toLocalDateTime(zone)
		val days = page.getElementsByClass("info-block_collapse")
		val lessons = days.flatMap { day ->
			val date = day.attribute("data-date")?.let { LocalDate.parseRus(it.value) }
				?: day.select(".info-block__header > span > span").text().parseRusShortDate(now)
				?: run {
					val dayOfWeek = day.select(".info-block__header > span").text().asDayOfWeek()!!
					val weekContainer = day.parents().firstOrNull { it.id().startsWith("week") }
					val period = 1 + (weekContainer?.siblingElements()?.size ?: 0)
					val week = weekContainer?.id()?.substringAfter('-')?.toInt() ?: 1
					val activeTimetableLink = timetableList.getElementsByClass("active").first()!!.child(0)
					val start = LocalDate.parse(activeTimetableLink.attr("href").substringAfter("start=").substringBefore('&'))

					val periodsBetween = start.until(now.date, DateTimeUnit.WEEK) / period
					val originalWeek = start.plus(periodsBetween * period + week - 1, DateTimeUnit.WEEK)
					originalWeek.weekStartDay().plus(dayOfWeek.ordinal, DateTimeUnit.DAY)
				}

			if (date.daysUntil(now.date) <= 7) { // оставляем расписание максимум на неделю назад (если предоставляется)
				day.getElementsByClass("timetable__list-timeslot").flatMap { slot ->
					val (timeElement, contentElement) = slot.children()

					val time = timeElement.text().substringAfter(", ")
					val (start, end) = time.trim().split(" — ").map { it.asLocalTime()!!.atDate(date) }

					contentElement.children().chunked(3).map { (title, about) ->
						val type = title.text().asLessonType()
						val name = title.nextSibling()!!.toString().trim()

						val professors = about.getElementsByClass("icon-academic-cap").map {
							EntryInfo(it.attr("title"), YOUR_PLACE.defaultUrl.dropLast(1) + it.attr("href"))
						}.toSet()

						val locations = about.getElementsByClass("icon-location").map {
							EntryInfo(it.attr("title"), it.attribute("href")?.let { YOUR_PLACE.defaultUrl + it.value })
						}.toSet()

						val groups = about.getElementsByClass("icon-community").map {
							val name = it.lastChild().toString().trim()
							val url = it.attribute("href")?.let { YOUR_PLACE.defaultUrl + it.value } ?: url
							EntryInfo(name, url)
						}
						val (sub, ordinal) = groups.partition { "п/гр." in it.name }

						Lesson(
							name = name,
							type = type,
							startTime = start,
							durationInMinutes = start.toInstant(zone).until(end.toInstant(zone), MINUTE).toInt(),
							groups = ordinal.toSet(),
							subgroups = sub.map { it.name.substringAfter('.').toInt() }.toSet(),
							persons = professors,
							classrooms = locations,
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
private fun String.parseRusShortDate(now: LocalDateTime): LocalDate? {
	return if (this.isNotEmpty()) { // ошибка буквально только в одном расписании
		val day = this.substringBefore(' ').toInt()
		val month = this.substringAfter(' ').asMonth()!!
		val year = if (month.ordinal - now.monthNumber > 7) now.year + 1 else now.year
		LocalDate(year, month, day)
	} else {
		null
	}
}
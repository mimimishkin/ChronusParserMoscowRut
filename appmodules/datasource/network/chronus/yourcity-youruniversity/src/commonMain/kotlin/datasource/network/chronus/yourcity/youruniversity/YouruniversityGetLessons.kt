package datasource.network.chronus.yourcity.youruniversity

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import library.logger.LogType
import library.logger.log
import model.chronus.EntryInfo
import model.chronus.Lesson
import model.chronus.Place.YOUR_PLACE
import model.chronus.Schedule
import model.chronus.ScheduleType
import model.chronus.asLessonType

suspend fun getLessons(client: HttpClient, json: Json, schedule: Schedule): List<Lesson>? {
	val id = schedule.url.substringAfterLast('/')
	val url = when (schedule.type) {
		ScheduleType.GROUP -> SCHEDULES + "group/"
		ScheduleType.PERSON -> SCHEDULES + "person/"
		ScheduleType.CLASSROOM -> SCHEDULES + "room/"
		ScheduleType.OTHER -> throw IllegalArgumentException("Unknown type of schedule")
	}

	val timetables = try {
		client.get(url + id).body<Timetables>().timetables
	} catch (e: Exception) {
		log(LogType.NetworkClientError, e)
		e.printStackTrace()
		return null
	}

	val schedules = timetables.map { timetable ->
		try {
			client.get(url + "$id/${timetable.id}").body<RutSchedule>()
		} catch (e: Exception) {
			log(LogType.NetworkClientError, e)
			e.printStackTrace()
			return null
		}
	}

	val events = schedules.flatMap { it.periodicContent?.events.orEmpty() + it.nonPeriodicContent?.events.orEmpty() }
	return events.map { event ->
		val zone = TimeZone.currentSystemDefault()
		Lesson(
			name = event.name,
			type = event.typeName.asLessonType(),
			startTime = event.start.toLocalDateTime(zone),
			durationInMinutes = (event.end - event.start).inWholeMinutes.toInt(),
			groups = event.groups.mapTo(mutableSetOf()) { EntryInfo(it.name, YOUR_PLACE.defaultUrl + it.id) },
			persons = event.lecturers.mapTo(mutableSetOf()) { EntryInfo(it.shortFio, YOUR_PLACE.defaultUrl + it.id) },
			classrooms = event.rooms.mapTo(mutableSetOf()) { EntryInfo(it.hint, YOUR_PLACE.defaultUrl + it.id) },
		)
	}
}
package datasource.network.chronus.yourcity.youruniversity

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import model.chronus.Place.YOUR_PLACE

val GROUP_CATALOG = YOUR_PLACE.defaultUrl + "data-service/data/timetable/groups-catalog"
val SCHEDULES = YOUR_PLACE.defaultUrl + "api/v1b/public/timetable/v2/"

@Serializable
data class Institutes(
	val institutes: List<Institute>
)

@Serializable
data class Institute(
	val name: String, val abbreviation: String, val courses: List<Course>
)

@Serializable
data class Course(
	val course: Int, val specialties: List<Specialty>
)

@Serializable
data class Specialty(
	val name: String, val abbreviation: String, val groups: List<Group>
)

@Serializable
data class Group(
	val id: Int, val name: String
)

@Serializable
data class Timetables(
	val timetables: List<Timetable>
)

@Serializable
enum class TimetableType(val type: String) {
	SESSION("SESSION"), PERIODIC("PERIODIC"), NON_PERIODIC("NON_PERIODIC")
}

@Serializable
data class Timetable(
	val id: String,
	val type: TimetableType,
	val startDate: LocalDate,
	val endDate: LocalDate,
)

@Serializable
data class RutSchedule(
	val timetable: Timetable,
	val periodicContent: PeriodicContent?,
	val nonPeriodicContent: NonPeriodicContent?
)

@Serializable
sealed class Content {
	abstract val events: List<Event>
}

@Serializable
data class PeriodicContent(
	override val events: List<PeriodicEvent>, val recurrence: FrequencyRule
) : Content()

@Serializable
data class NonPeriodicContent(
	override val events: List<NonPeriodicEvent>
) : Content()

@Serializable
sealed class Event {
	abstract val name: String
	abstract val typeName: String
	@SerialName("startDatetime") abstract val start: Instant
	@SerialName("endDatetime") abstract val end: Instant
	abstract val lecturers: List<Lecturer>
	abstract val rooms: List<Room>
	abstract val groups: List<GroupInfo>
}

@Serializable
data class Lecturer(
	val id: Int,
	val shortFio: String,
	val fullFio: String,
	val description: String,
)

@Serializable
data class Room(
	val id: Int,
	val name: String,
	val hint: String
)

@Serializable
data class GroupInfo(
	val id: Int,
	val name: String
)

@Serializable
enum class Frequency(val frequency: String) {
	WEEKLY("WEEKLY")
}

@Serializable
data class FrequencyRule(
	val frequency: Frequency,
	val interval: Int,
	val currentNumber: Int? = null,
	val periods: List<Period>? = null
)

@Serializable
data class Period(
	val number: Int,
	val name: String,
	val current: Boolean
)

@Serializable
data class PeriodicEvent(
	override val name: String,
	override val typeName: String,
	@SerialName("startDatetime") override val start: Instant,
	@SerialName("endDatetime") override val end: Instant,
	override val lecturers: List<Lecturer>,
	override val rooms: List<Room>,
	override val groups: List<GroupInfo>,
	val timeSlotName: String,
	val periodNumber: Int,
	val recurrenceRule: FrequencyRule
) : Event()

@Serializable
data class NonPeriodicEvent(
	override val name: String,
	override val typeName: String,
	@SerialName("startDatetime") override val start: Instant,
	@SerialName("endDatetime") override val end: Instant,
	override val lecturers: List<Lecturer>,
	override val rooms: List<Room>,
	override val groups: List<GroupInfo>,
) : Event()
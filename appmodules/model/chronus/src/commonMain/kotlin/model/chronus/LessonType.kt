package model.chronus

sealed interface LessonType {
	data object Lecture : LessonType // лекция

	data object Practice : LessonType // практика

	data object LabWork : LessonType // лабораторная работа

	data object Project : LessonType // проектная деятельность

	data object Exam : LessonType // экзамен

	data object CourseCredit : LessonType // зачёт

	data object Consultation : LessonType // консультация

	data class Other(val name: String = "") : LessonType // любой другой тип
}

fun LessonType.Other.correctNameOrNull(): String? =
	this.name.replaceFirstChar { it.uppercase() }.takeIf { it.isNotBlank() }

fun LessonType.asString(): String? = when (this) {
	LessonType.Lecture -> "Лекция"
	LessonType.Practice -> "Практика"
	LessonType.LabWork -> "Лабораторная работа"
	LessonType.Project -> "Проектная деятельность"
	LessonType.Exam -> "Экзамен"
	LessonType.CourseCredit -> "Зачёт"
	LessonType.Consultation -> "Консультация"
	is LessonType.Other -> correctNameOrNull()
}

val allTypes = mutableSetOf<String?>()

fun String?.asLessonType(): LessonType = when (val lower = this.also {
	val size = allTypes.size
	allTypes += it
	if (allTypes.size > size) {
		println(allTypes.joinToString("\n").encodeToByteArray().joinToString(prefix = "[", postfix = "]"))
	}
}?.trim()?.lowercase() ?: "") {
	// обратите внимание: тип должен быть указан маленькими буквами

	"лекция",
	-> LessonType.Lecture

	"практика",
	"практическое занятие", // БГУ Иркутск
	"практические занятия", // БГУ Усть-Илимск
	"практические (семинарские) занятия", // ИРГУПС
	-> LessonType.Practice

	"лабораторная работа",
	"лабораторная", // ИГУ
	"лаб.-практич.занятия", // БГУ Усть-Илимск
	-> LessonType.LabWork

	"проект",
	"проектная деятельность",
	-> LessonType.Project

	"экзамен",
	"экзамены",
	-> LessonType.Exam

	"зачет",
	"зачёт",
	-> LessonType.CourseCredit

	"консультация",
	-> LessonType.Consultation

	else -> {
		if (lower.contains(';') || lower.contains(',')) {
			val types = lower.split(';', ',').map { it.asLessonType() }
			LessonType.Other(types.joinToString { it.asString()!! }) /* LessonType.Multiple(types) */
		} else if (lower.contains("(вебинар)")) {
			lower.substringBefore(" (вебинар)").asLessonType()/*.copy { format = Webinar }*/
		} else if (lower.contains("(конференция)")) {
			lower.substringBefore(" (конференция)").asLessonType()/*.copy { format = Conference }*/
		} else {
			LessonType.Other(lower)
		}
	}

	// если ни один LessonType не подходит к некоторым типам занятия в вашем ВУЗе,
	// оставьте эти типы ниже, я обработаю их самостоятельно:
	// "ваш_неподходящий_тип_1", "ваш_неподходящий_тип_2"
// "дифференцированный зачет", "учебная практика", "производственная практика", "научно-исследовательская работа",
// "курсовая работа", "защита", а также "что-то (вебинар)", "что-то (конференция)", несколько типов
}

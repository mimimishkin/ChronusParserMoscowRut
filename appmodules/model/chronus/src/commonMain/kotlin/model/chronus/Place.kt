package model.chronus

enum class Place(
	val cyrillicName: String,
	val city: City,
	val contributor: Contributor,
	val defaultUrl: String,
	val minSearchChars: Int?, // if null, query is not needed, otherwise at least 1 char is needed
	val searchType: SearchType,
	val lessonDurationInMinutes: Int = 90,
	val apiCredits: Pair<String, String>? = null, // name and url to your schedule api with OSS license (if needed)
) {
	IRKUTSK_IGU_IMIT(
		cyrillicName = "ИМИТ ИГУ",
		city = City.IRKUTSK,
		contributor = Contributor.MXKMN,
		defaultUrl = "https://raspmath.isu.ru/",
		minSearchChars = null,
		searchType = SearchType.BY_GROUP_PERSON_PLACE,
	),
	IRKUTSK_IRNITU(
		cyrillicName = "ИРНИТУ",
		city = City.IRKUTSK,
		contributor = Contributor.MXKMN,
		defaultUrl = "https://istu.edu/schedule/",
		minSearchChars = 2,
		searchType = SearchType.BY_GROUP_PERSON_PLACE,
	),
	YOUR_PLACE(
		cyrillicName = "РУТ (МИИТ)",
		city = City.MOSCOW,
		contributor = Contributor.YOU,
		defaultUrl = "https://rut-miit.ru/",
		minSearchChars = 0,
		searchType = SearchType.BY_GROUP_PERSON
	),
}

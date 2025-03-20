package datasource.network.chronus.yourcity.youruniversity

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import library.logger.LogType
import library.logger.log
import model.chronus.Place.YOUR_PLACE
import model.chronus.Schedule
import model.chronus.ScheduleType.GROUP
import model.chronus.ScheduleType.PERSON

suspend fun getSearchResults(client: HttpClient, json: Json, query: String): List<Schedule>? {
	val institutes = try {
		client.get(GROUP_CATALOG).body<Institutes>().institutes
	} catch (e: Exception) {
		log(LogType.NetworkClientError, e)
		return null
	}

	val groups = institutes.flatMap { it.courses.flatMap { it.specialties.flatMap { it.groups } } }
	val schedules = groups.mapTo(mutableListOf()) {
		Schedule(
			name = it.name,
			type = GROUP,
			place = YOUR_PLACE,
			url = YOUR_PLACE.defaultUrl + "groups/${it.id}"
		)
	}

	// TODO: для получения списка преподавателей и аудиторий нет api,
	//  поэтому парсим сайт с преподавателями (для аудиторий даже этого нет).
	//  Но он разбит на страницы => передавать сюда query
	val professors = searchProfessors(client, query)
	if (professors != null) {
		schedules += professors
	}

	return schedules
}

private suspend fun searchProfessors(client: HttpClient, query: String): List<Schedule>? {
	val page = try {
		client.get(YOUR_PLACE.defaultUrl + "depts/37/professors?query=" + query).bodyAsText()
	} catch (e: Exception) {
		log(LogType.NetworkClientError, e)
		return null
	}

	return try {
		withContext(Dispatchers.Default) {
			Ksoup.parse(page).getElementsByClass("search__people").map {
				val name = it.getElementsByTag("a")
				Schedule(
					name = name.text(),
					type = PERSON,
					place = YOUR_PLACE,
					url = YOUR_PLACE.defaultUrl + name.attr("href").substringAfterLast('/')
				)
			}
		}
	} catch (e: Exception) {
		log(LogType.ParseError, e)
		null
	}
}
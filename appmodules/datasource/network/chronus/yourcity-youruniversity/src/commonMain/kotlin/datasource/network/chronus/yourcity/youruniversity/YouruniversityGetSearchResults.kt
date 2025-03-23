package datasource.network.chronus.yourcity.youruniversity

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import library.logger.LogType
import library.logger.log
import model.chronus.Place.YOUR_PLACE
import model.chronus.Schedule
import model.chronus.ScheduleType.GROUP
import model.chronus.ScheduleType.PERSON

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun getSearchResults(client: HttpClient, json: Json, query: String): List<Schedule>? = coroutineScope {
	val groups = async { searchGroups(client, query) }
	val professors = async { searchProfessors(client, query) }
	if (groups.await() == null) {
		professors.cancel()
		return@coroutineScope null
	}
	if (professors.await() == null) {
		return@coroutineScope null
	}

	groups.getCompleted()!! + professors.getCompleted()!!
}

private suspend fun searchGroups(client: HttpClient, query: String): List<Schedule>? {
	val page = try {
		client.get(YOUR_PLACE.defaultUrl + "timetable/").bodyAsText()
	} catch (e: Exception) {
		log(LogType.NetworkClientError, e)
		return null
	}

	return try {
		withContext(Dispatchers.Default) {
			Ksoup.parse(page).getElementsByTag("a").mapNotNull {
				val link = it.attr("href")
				val name = it.text()
				if (link.startsWith("/timetable/") && name.contains(query, ignoreCase = true)) {
					Schedule(
						name = name.trim(),
						type = GROUP,
						place = YOUR_PLACE,
						url = YOUR_PLACE.defaultUrl.dropLast(1) + link
					)
				} else {
					null
				}
			}
		}
	} catch (e: Exception) {
		log(LogType.ParseError, e)
		null
	}
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
			Ksoup.parse(page).getElementsByTag("a").mapNotNull {
				val link = it.attr("href")
				if (link.startsWith("/people/")) {
					Schedule(
						name = it.text(),
						type = PERSON,
						place = YOUR_PLACE,
						url = YOUR_PLACE.defaultUrl.dropLast(1) + link + "/timetable"
					)
				} else {
					null
				}
			}
		}
	} catch (e: Exception) {
		log(LogType.ParseError, e)
		null
	}
}
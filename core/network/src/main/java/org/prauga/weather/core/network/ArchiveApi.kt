// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.prauga.weather.core.model.DailyHigh
import org.prauga.weather.core.model.Place

class ArchiveApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun dailyHighs(place: Place, start: LocalDate, end: LocalDate): String {
        val response =
            client.get("$baseUrl/v1/archive") {
                parameter("latitude", Place.format(place.latitude))
                parameter("longitude", Place.format(place.longitude))
                parameter("start_date", start.toString())
                parameter("end_date", end.toString())
                parameter("daily", "temperature_2m_max")
                parameter("timezone", "auto")
            }
        if (!response.status.isSuccess()) {
            throw ProviderHttpException(
                response.status.value,
                "Open-Meteo archive: ${response.status}",
            )
        }
        return response.bodyAsText()
    }

    companion object {
        const val BASE_URL = "https://archive-api.open-meteo.com"

        fun parse(body: String): List<DailyHigh> {
            val daily =
                WeatherJson.decodeFromString(ArchiveResponse.serializer(), body).daily
                    ?: return emptyList()
            return daily.time.indices.mapNotNull { i ->
                daily.temperatureMax.getOrNull(i)?.let {
                    DailyHigh(LocalDate.parse(daily.time[i]), it)
                }
            }
        }
    }
}

@Serializable internal data class ArchiveResponse(val daily: ArchiveDaily? = null)

@Serializable
internal data class ArchiveDaily(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
)

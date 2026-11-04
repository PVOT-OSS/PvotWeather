// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.time.ZoneId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.prauga.weather.core.model.Place

class GeocodingApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun search(name: String, language: String, count: Int = 10): List<Place> {
        val response =
            client.get("$baseUrl/v1/search") {
                parameter("name", name)
                parameter("count", count)
                parameter("language", language)
                parameter("format", "json")
            }
        if (!response.status.isSuccess()) {
            throw ProviderHttpException(response.status.value, "Geocoding: ${response.status}")
        }
        return parse(response.bodyAsText())
    }

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com"
        private const val MISSING_ELEVATION = -1000.0

        fun parse(body: String): List<Place> =
            WeatherJson.decodeFromString(GeoResponse.serializer(), body).results.mapNotNull { r ->
                val zone =
                    r.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                        ?: return@mapNotNull null
                Place.of(
                    name = r.name,
                    area = r.admin1,
                    latitude = r.latitude,
                    longitude = r.longitude,
                    elevation = r.elevation?.takeIf { it > MISSING_ELEVATION }?.toInt(),
                    zone = zone,
                    country = r.country,
                )
            }
    }
}

@Serializable internal data class GeoResponse(val results: List<GeoResult> = emptyList())

@Serializable
internal data class GeoResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val timezone: String? = null,
    val country: String? = null,
    @SerialName("admin1") val admin1: String? = null,
)

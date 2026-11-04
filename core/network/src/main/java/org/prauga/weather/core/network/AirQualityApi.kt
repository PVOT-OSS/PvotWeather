// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.prauga.weather.core.model.AirQuality
import org.prauga.weather.core.model.Place

class AirQualityApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun current(place: Place): String {
        val response =
            client.get("$baseUrl/v1/air-quality") {
                parameter("latitude", Place.format(place.latitude))
                parameter("longitude", Place.format(place.longitude))
                parameter("current", "us_aqi,pm2_5,pm10")
                parameter("timeformat", "unixtime")
            }
        if (!response.status.isSuccess()) {
            throw ProviderHttpException(
                response.status.value,
                "Open-Meteo air quality: ${response.status}",
            )
        }
        return response.bodyAsText()
    }

    companion object {
        const val BASE_URL = "https://air-quality-api.open-meteo.com"

        fun parse(body: String): AirQuality? {
            val current =
                WeatherJson.decodeFromString(AirResponse.serializer(), body).current ?: return null
            val aqi = current.usAqi ?: return null
            return AirQuality(
                Instant.ofEpochSecond(current.time),
                aqi.toInt(),
                current.pm25,
                current.pm10,
            )
        }
    }
}

@Serializable internal data class AirResponse(val current: AirCurrent? = null)

@Serializable
internal data class AirCurrent(
    val time: Long,
    @SerialName("us_aqi") val usAqi: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null,
)

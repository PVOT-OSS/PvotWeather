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
import org.prauga.weather.core.model.Pollen

class AirQualityApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun current(place: Place): String {
        val response =
            client.get("$baseUrl/v1/air-quality") {
                parameter("latitude", Place.format(place.latitude))
                parameter("longitude", Place.format(place.longitude))
                parameter("current", CURRENT)
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
        private const val CURRENT =
            "us_aqi,european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide," +
                "alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen"

        fun parse(body: String): AirQuality? {
            val current =
                WeatherJson.decodeFromString(AirResponse.serializer(), body).current ?: return null
            val aqi = current.usAqi ?: return null
            return AirQuality(
                Instant.ofEpochSecond(current.time),
                aqi.toInt(),
                current.pm25,
                current.pm10,
                current.europeanAqi?.toInt(),
                current.ozone,
                current.nitrogenDioxide,
                current.sulphurDioxide,
                current.carbonMonoxide,
                buildMap {
                    current.alder?.let { put(Pollen.ALDER, it) }
                    current.birch?.let { put(Pollen.BIRCH, it) }
                    current.grass?.let { put(Pollen.GRASS, it) }
                    current.mugwort?.let { put(Pollen.MUGWORT, it) }
                    current.olive?.let { put(Pollen.OLIVE, it) }
                    current.ragweed?.let { put(Pollen.RAGWEED, it) }
                },
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
    @SerialName("european_aqi") val europeanAqi: Double? = null,
    val ozone: Double? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @SerialName("sulphur_dioxide") val sulphurDioxide: Double? = null,
    @SerialName("carbon_monoxide") val carbonMonoxide: Double? = null,
    @SerialName("alder_pollen") val alder: Double? = null,
    @SerialName("birch_pollen") val birch: Double? = null,
    @SerialName("grass_pollen") val grass: Double? = null,
    @SerialName("mugwort_pollen") val mugwort: Double? = null,
    @SerialName("olive_pollen") val olive: Double? = null,
    @SerialName("ragweed_pollen") val ragweed: Double? = null,
)

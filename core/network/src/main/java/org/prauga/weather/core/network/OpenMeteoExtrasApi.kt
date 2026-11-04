// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.time.Instant
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.prauga.weather.core.model.EnsembleHour
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.River

data class Marine(
    val waveHeight: Double?,
    val waveDirection: Double?,
    val wavePeriod: Double?,
    val seaTemperature: Double?,
)

class OpenMeteoExtrasApi(private val client: HttpClient) {

    suspend fun marine(place: Place): String =
        get("https://marine-api.open-meteo.com/v1/marine", place) {
            parameter("current", "wave_height,wave_direction,wave_period,sea_surface_temperature")
            parameter("timeformat", "unixtime")
        }

    suspend fun flood(place: Place): String =
        get("https://flood-api.open-meteo.com/v1/flood", place) {
            parameter("daily", "river_discharge,river_discharge_mean,river_discharge_max")
            parameter("forecast_days", 7)
        }

    suspend fun ensemble(place: Place): String =
        get("https://ensemble-api.open-meteo.com/v1/ensemble", place) {
            parameter("hourly", "temperature_2m")
            parameter("models", ENSEMBLE_MODEL)
            parameter("forecast_days", 3)
            parameter("timeformat", "unixtime")
        }

    private suspend fun get(
        url: String,
        place: Place,
        extra: HttpRequestBuilder.() -> Unit,
    ): String {
        val response =
            client.get(url) {
                parameter("latitude", Place.format(place.latitude))
                parameter("longitude", Place.format(place.longitude))
                extra()
            }
        if (!response.status.isSuccess()) {
            throw ProviderHttpException(response.status.value, "Open-Meteo: ${response.status}")
        }
        return response.bodyAsText()
    }

    companion object {
        const val ENSEMBLE_MODEL = "icon_seamless"

        fun parseMarine(body: String): Marine? {
            val c =
                WeatherJson.decodeFromString(MarineResponse.serializer(), body).current
                    ?: return null
            if (c.waveHeight == null && c.seaTemperature == null) return null
            return Marine(c.waveHeight, c.waveDirection, c.wavePeriod, c.seaTemperature)
        }

        fun parseFlood(body: String, today: LocalDate): River? {
            val d =
                WeatherJson.decodeFromString(FloodResponse.serializer(), body).daily ?: return null
            val i = d.time.indexOf(today.toString()).takeIf { it >= 0 } ?: return null
            val discharge = d.discharge.getOrNull(i) ?: return null
            return River(today, discharge, d.mean.getOrNull(i), d.max.getOrNull(i))
        }

        fun parseEnsemble(body: String): List<EnsembleHour> {
            val hourly =
                WeatherJson.parseToJsonElement(body).jsonObject["hourly"]?.jsonObject
                    ?: return emptyList()
            val times =
                hourly["time"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull }
                    ?: return emptyList()
            val members =
                hourly.filterKeys { it.startsWith("temperature_2m") }.values.map { it as JsonArray }
            return times.indices.mapNotNull { i ->
                val values =
                    members.mapNotNull { it.getOrNull(i)?.jsonPrimitive?.doubleOrNull }.sorted()
                if (values.size < MIN_MEMBERS) return@mapNotNull null
                EnsembleHour(
                    Instant.ofEpochSecond(times[i]),
                    percentile(values, 0.1),
                    percentile(values, 0.9),
                    values.size,
                )
            }
        }

        internal fun percentile(sorted: List<Double>, p: Double): Double {
            val rank = p * (sorted.size - 1)
            val lo = sorted[floor(rank).toInt()]
            val hi = sorted[ceil(rank).toInt()]
            return lo + (hi - lo) * (rank - floor(rank))
        }

        private const val MIN_MEMBERS = 5
    }
}

@Serializable internal data class MarineResponse(val current: MarineCurrent? = null)

@Serializable
internal data class MarineCurrent(
    @SerialName("wave_height") val waveHeight: Double? = null,
    @SerialName("wave_direction") val waveDirection: Double? = null,
    @SerialName("wave_period") val wavePeriod: Double? = null,
    @SerialName("sea_surface_temperature") val seaTemperature: Double? = null,
)

@Serializable internal data class FloodResponse(val daily: FloodDaily? = null)

@Serializable
internal data class FloodDaily(
    val time: List<String> = emptyList(),
    @SerialName("river_discharge") val discharge: List<Double?> = emptyList(),
    @SerialName("river_discharge_mean") val mean: List<Double?> = emptyList(),
    @SerialName("river_discharge_max") val max: List<Double?> = emptyList(),
)

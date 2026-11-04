// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.prauga.weather.core.model.MetAirQuality
import org.prauga.weather.core.model.Moon

data class Ocean(
    val time: Instant,
    val waveHeight: Double?,
    val waveDirection: Double?,
    val seaTemperature: Double?,
    val currentSpeed: Double?,
    val currentDirection: Double?,
)

object MetMoreParser {

    fun moon(body: String): Moon? {
        val p =
            WeatherJson.decodeFromString(MoonResponse.serializer(), body).properties ?: return null
        val phase = p.moonphase ?: return null
        return Moon(p.moonrise?.time?.let(::instant), p.moonset?.time?.let(::instant), phase)
    }

    fun airQuality(body: String, now: Instant): MetAirQuality? {
        val response = WeatherJson.decodeFromString(MetAqResponse.serializer(), body)
        val step =
            response.data.time
                .mapNotNull { t ->
                    runCatching { Instant.parse(t.from) }.getOrNull()?.let { it to t }
                }
                .filter { (time, _) -> Duration.between(time, now).abs() <= Duration.ofMinutes(90) }
                .minByOrNull { (time, _) -> Duration.between(time, now).abs() }
                ?.second ?: return null
        val v = step.variables
        val index = v["AQI"]?.value ?: return null
        return MetAirQuality(
            Instant.parse(step.from),
            index,
            v["pm25_concentration"]?.value,
            v["pm10_concentration"]?.value,
            v["no2_concentration"]?.value,
            v["o3_concentration"]?.value,
            response.meta?.superlocation?.name ?: response.meta?.location?.name,
        )
    }

    fun ocean(body: String, now: Instant): Ocean? {
        val steps =
            WeatherJson.decodeFromString(OceanResponse.serializer(), body).properties.timeseries
        val step =
            steps
                .minByOrNull { Duration.between(Instant.parse(it.time), now).abs() }
                ?.takeIf {
                    Duration.between(Instant.parse(it.time), now).abs() <= Duration.ofHours(2)
                } ?: return null
        val d = step.data.instant.details
        return Ocean(
            Instant.parse(step.time),
            d.waveHeight,
            d.waveDirection,
            d.seaTemperature,
            d.currentSpeed,
            d.currentDirection,
        )
    }

    private fun instant(value: String): Instant? =
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
}

@Serializable internal data class MoonResponse(val properties: MoonProperties? = null)

@Serializable
internal data class MoonProperties(
    val moonrise: MoonEvent? = null,
    val moonset: MoonEvent? = null,
    val moonphase: Double? = null,
)

@Serializable internal data class MoonEvent(val time: String? = null)

@Serializable internal data class MetAqResponse(val meta: MetAqMeta? = null, val data: MetAqData)

@Serializable
internal data class MetAqMeta(
    val location: MetAqLocation? = null,
    val superlocation: MetAqLocation? = null,
)

@Serializable internal data class MetAqLocation(val name: String? = null)

@Serializable internal data class MetAqData(val time: List<MetAqStep> = emptyList())

@Serializable
internal data class MetAqStep(
    val from: String,
    val variables: Map<String, MetAqValue> = emptyMap(),
)

@Serializable internal data class MetAqValue(val value: Double? = null)

@Serializable internal data class OceanResponse(val properties: OceanProperties)

@Serializable internal data class OceanProperties(val timeseries: List<OceanStep> = emptyList())

@Serializable internal data class OceanStep(val time: String, val data: OceanData)

@Serializable internal data class OceanData(val instant: OceanInstant)

@Serializable internal data class OceanInstant(val details: OceanDetails)

@Serializable
internal data class OceanDetails(
    @SerialName("sea_surface_wave_height") val waveHeight: Double? = null,
    @SerialName("sea_surface_wave_from_direction") val waveDirection: Double? = null,
    @SerialName("sea_water_temperature") val seaTemperature: Double? = null,
    @SerialName("sea_water_speed") val currentSpeed: Double? = null,
    @SerialName("sea_water_to_direction") val currentDirection: Double? = null,
)

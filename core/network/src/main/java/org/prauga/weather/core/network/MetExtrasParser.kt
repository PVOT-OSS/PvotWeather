// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.prauga.weather.core.model.Alert
import org.prauga.weather.core.model.AlertLevel
import org.prauga.weather.core.model.Nowcast
import org.prauga.weather.core.model.PrecipitationStep
import org.prauga.weather.core.model.Provider

object MetExtrasParser {

    fun nowcast(body: String): Nowcast? {
        val properties = WeatherJson.decodeFromString(NowcastResponse.serializer(), body).properties
        if (properties.meta.radarCoverage != "ok") return null
        val steps =
            properties.timeseries.mapNotNull { step ->
                step.data.instant.details.precipitationRate?.let {
                    PrecipitationStep(Instant.parse(step.time), it)
                }
            }
        return Nowcast(Provider.MET_NORWAY, steps).takeIf { steps.isNotEmpty() }
    }

    fun alerts(body: String): List<Alert> =
        WeatherJson.decodeFromString(AlertsResponse.serializer(), body).features.mapNotNull {
            feature ->
            val p = feature.properties
            val level =
                when (p.awarenessLevel?.split(";")?.getOrNull(1)?.trim()) {
                    "yellow" -> AlertLevel.YELLOW
                    "orange" -> AlertLevel.ORANGE
                    "red" -> AlertLevel.RED
                    else -> return@mapNotNull null
                }
            Alert(
                id = p.id,
                event = p.eventAwarenessName ?: p.event.orEmpty(),
                level = level,
                area = p.area.orEmpty(),
                description = p.description.orEmpty().trim(),
                instruction = p.instruction?.trim()?.ifEmpty { null },
                ends =
                    p.eventEndingTime?.let {
                        runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
                    },
                url = p.web,
            )
        }
}

@Serializable internal data class NowcastResponse(val properties: NowcastProperties)

@Serializable
internal data class NowcastProperties(
    val meta: NowcastMeta,
    val timeseries: List<NowcastStep> = emptyList(),
)

@Serializable
internal data class NowcastMeta(@SerialName("radar_coverage") val radarCoverage: String? = null)

@Serializable internal data class NowcastStep(val time: String, val data: NowcastData)

@Serializable internal data class NowcastData(val instant: NowcastInstant)

@Serializable internal data class NowcastInstant(val details: NowcastDetails)

@Serializable
internal data class NowcastDetails(
    @SerialName("precipitation_rate") val precipitationRate: Double? = null
)

@Serializable internal data class AlertsResponse(val features: List<AlertFeature> = emptyList())

@Serializable internal data class AlertFeature(val properties: AlertProperties)

@Serializable
internal data class AlertProperties(
    val id: String,
    val event: String? = null,
    val eventAwarenessName: String? = null,
    @SerialName("awareness_level") val awarenessLevel: String? = null,
    val area: String? = null,
    val description: String? = null,
    val instruction: String? = null,
    val eventEndingTime: String? = null,
    val web: String? = null,
)

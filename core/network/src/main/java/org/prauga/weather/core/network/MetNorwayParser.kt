// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.prauga.weather.core.model.Condition
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.model.ProviderDay
import org.prauga.weather.core.model.ProviderForecast
import org.prauga.weather.core.model.ProviderHour

object MetNorwayParser {

    fun parse(body: String, zone: ZoneId): ProviderForecast {
        val steps =
            WeatherJson.decodeFromString(MetResponse.serializer(), body).properties.timeseries
        val hours =
            steps
                .filter { it.data.next1 != null }
                .map { step ->
                    val d = step.data.instant.details
                    val next = step.data.next1
                    ProviderHour(
                        time = Instant.parse(step.time),
                        temperature = d.airTemperature,
                        apparentTemperature = d.apparentTemperature,
                        humidity = d.humidity,
                        dewPoint = d.dewPoint,
                        pressure = d.pressure,
                        cloudCover = d.cloudCover,
                        windSpeed = d.windSpeed,
                        windDirection = d.windDirection,
                        windGust = d.windGust,
                        uvIndex = d.uvClearSky,
                        precipitation = next?.details?.precipitationAmount,
                        precipitationProbability = next?.details?.precipitationProbability,
                        condition = Conditions.fromMetSymbol(next?.summary?.symbolCode),
                        isDay = Conditions.isDayFromMetSymbol(next?.summary?.symbolCode),
                        temperatureLow = d.temperatureP10,
                        temperatureHigh = d.temperatureP90,
                        windLow = d.windP10,
                        windHigh = d.windP90,
                        precipitationMin = next?.details?.precipitationMin,
                        precipitationMax = next?.details?.precipitationMax,
                        thunderProbability = next?.details?.thunderProbability,
                        fog = d.fog,
                        cloudLow = d.cloudLow,
                        cloudMid = d.cloudMid,
                        cloudHigh = d.cloudHigh,
                    )
                }
        return ProviderForecast(Provider.MET_NORWAY, hours.firstOrNull(), hours, days(steps, zone))
    }

    private fun days(steps: List<MetStep>, zone: ZoneId): List<ProviderDay> {
        val byDate = steps.groupBy { Instant.parse(it.time).atZone(zone).toLocalDate() }
        val first = byDate.keys.minOrNull()
        return byDate.mapNotNull { (date, daySteps) ->
            val coveredUntil = daySteps.maxOf { it.end() }
            val evening = date.atTime(18, 0).atZone(zone).toInstant()
            if (date != first && coveredUntil.isBefore(evening)) return@mapNotNull null
            day(date, daySteps)
        }
    }

    private fun day(date: LocalDate, steps: List<MetStep>): ProviderDay {
        val temps = buildList {
            steps.forEach { s ->
                s.data.instant.details.airTemperature?.let(::add)
                s.data.next6?.details?.airTemperatureMax?.let(::add)
                s.data.next6?.details?.airTemperatureMin?.let(::add)
            }
        }
        // Hourly steps carry 1 h amounts, 6-hourly steps 6 h ones; never add both.
        val amounts =
            steps.mapNotNull {
                it.data.next1?.details?.precipitationAmount
                    ?: it.data.next6?.details?.precipitationAmount
            }
        val minimums =
            steps.mapNotNull {
                it.data.next1?.details?.precipitationMin ?: it.data.next6?.details?.precipitationMin
            }
        val maximums =
            steps.mapNotNull {
                it.data.next1?.details?.precipitationMax ?: it.data.next6?.details?.precipitationMax
            }
        val probabilities =
            steps.mapNotNull {
                it.data.next1?.details?.precipitationProbability
                    ?: it.data.next6?.details?.precipitationProbability
            }
        val condition =
            steps
                .mapNotNull {
                    Conditions.fromMetSymbol((it.data.next1 ?: it.data.next6)?.summary?.symbolCode)
                }
                .fold(null as Condition?) { worst, c -> Condition.mostSevere(worst, c) }
        return ProviderDay(
            date = date,
            temperatureMax = temps.maxOrNull(),
            temperatureMin = temps.minOrNull(),
            precipitationSum = if (amounts.isEmpty()) null else amounts.sum(),
            precipitationProbability = probabilities.maxOrNull(),
            condition = condition,
            precipitationMin =
                minimums.takeIf { it.size == amounts.size && it.isNotEmpty() }?.sum(),
            precipitationMax = maximums.takeIf { it.size == amounts.size && it.isNotEmpty() }?.sum(),
        )
    }

    private fun MetStep.end(): Instant {
        val start = Instant.parse(time)
        return when {
            data.next1 != null -> start + Duration.ofHours(1)
            data.next6 != null -> start + Duration.ofHours(6)
            data.next12 != null -> start + Duration.ofHours(12)
            else -> start
        }
    }
}

@Serializable internal data class MetResponse(val properties: MetProperties)

@Serializable internal data class MetProperties(val timeseries: List<MetStep>)

@Serializable internal data class MetStep(val time: String, val data: MetData)

@Serializable
internal data class MetData(
    val instant: MetInstant,
    @SerialName("next_1_hours") val next1: MetPeriod? = null,
    @SerialName("next_6_hours") val next6: MetPeriod? = null,
    @SerialName("next_12_hours") val next12: MetPeriod? = null,
)

@Serializable internal data class MetInstant(val details: MetInstantDetails)

@Serializable
internal data class MetInstantDetails(
    @SerialName("air_temperature") val airTemperature: Double? = null,
    @SerialName("apparent_air_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity") val humidity: Double? = null,
    @SerialName("dew_point_temperature") val dewPoint: Double? = null,
    @SerialName("air_pressure_at_sea_level") val pressure: Double? = null,
    @SerialName("cloud_area_fraction") val cloudCover: Double? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null,
    @SerialName("wind_from_direction") val windDirection: Double? = null,
    @SerialName("wind_speed_of_gust") val windGust: Double? = null,
    @SerialName("ultraviolet_index_clear_sky") val uvClearSky: Double? = null,
    @SerialName("air_temperature_percentile_10") val temperatureP10: Double? = null,
    @SerialName("air_temperature_percentile_90") val temperatureP90: Double? = null,
    @SerialName("wind_speed_percentile_10") val windP10: Double? = null,
    @SerialName("wind_speed_percentile_90") val windP90: Double? = null,
    @SerialName("fog_area_fraction") val fog: Double? = null,
    @SerialName("cloud_area_fraction_low") val cloudLow: Double? = null,
    @SerialName("cloud_area_fraction_medium") val cloudMid: Double? = null,
    @SerialName("cloud_area_fraction_high") val cloudHigh: Double? = null,
)

@Serializable
internal data class MetPeriod(
    val summary: MetSummary? = null,
    val details: MetPeriodDetails? = null,
)

@Serializable
internal data class MetSummary(@SerialName("symbol_code") val symbolCode: String? = null)

@Serializable
internal data class MetPeriodDetails(
    @SerialName("precipitation_amount") val precipitationAmount: Double? = null,
    @SerialName("probability_of_precipitation") val precipitationProbability: Double? = null,
    @SerialName("precipitation_amount_min") val precipitationMin: Double? = null,
    @SerialName("precipitation_amount_max") val precipitationMax: Double? = null,
    @SerialName("probability_of_thunder") val thunderProbability: Double? = null,
    @SerialName("air_temperature_max") val airTemperatureMax: Double? = null,
    @SerialName("air_temperature_min") val airTemperatureMin: Double? = null,
)

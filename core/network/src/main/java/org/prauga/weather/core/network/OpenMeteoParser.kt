// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.prauga.weather.core.model.PrecipitationStep
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.model.ProviderDay
import org.prauga.weather.core.model.ProviderForecast
import org.prauga.weather.core.model.ProviderHour

object OpenMeteoParser {

    fun parse(body: String): ProviderForecast {
        val response = WeatherJson.decodeFromString(OmResponse.serializer(), body)
        val zone = ZoneId.of(response.timezone)
        val hourly = response.hourly
        val hours =
            hourly?.time?.indices?.map { i ->
                ProviderHour(
                    time = Instant.ofEpochSecond(hourly.time[i]),
                    temperature = hourly.temperature.at(i),
                    apparentTemperature = hourly.apparentTemperature.at(i),
                    humidity = hourly.humidity.at(i),
                    dewPoint = hourly.dewPoint.at(i),
                    pressure = hourly.pressure.at(i),
                    cloudCover = hourly.cloudCover.at(i),
                    windSpeed = hourly.windSpeed.at(i),
                    windDirection = hourly.windDirection.at(i),
                    windGust = hourly.windGust.at(i),
                    uvIndex = hourly.uvIndex.at(i),
                    visibility = hourly.visibility.at(i),
                    precipitation = hourly.precipitation.at(i),
                    precipitationProbability = hourly.precipitationProbability.at(i),
                    condition = Conditions.fromWmo(hourly.weatherCode.at(i)?.toInt()),
                    isDay = hourly.isDay.at(i)?.let { it > 0 },
                )
            } ?: emptyList()
        val daily = response.daily
        val days =
            daily?.time?.indices?.map { i ->
                ProviderDay(
                    date = Instant.ofEpochSecond(daily.time[i]).atZone(zone).toLocalDate(),
                    temperatureMax = daily.temperatureMax.at(i),
                    temperatureMin = daily.temperatureMin.at(i),
                    precipitationSum = daily.precipitationSum.at(i),
                    precipitationProbability = daily.precipitationProbabilityMax.at(i),
                    condition = Conditions.fromWmo(daily.weatherCode.at(i)?.toInt()),
                    uvIndexMax = daily.uvIndexMax.at(i),
                    sunrise = daily.sunrise.at(i)?.let { Instant.ofEpochSecond(it.toLong()) },
                    sunset = daily.sunset.at(i)?.let { Instant.ofEpochSecond(it.toLong()) },
                )
            } ?: emptyList()
        val current =
            response.current?.let { c ->
                val hour = hours.lastOrNull { !it.time.isAfter(Instant.ofEpochSecond(c.time)) }
                ProviderHour(
                    time = Instant.ofEpochSecond(c.time),
                    temperature = c.temperature,
                    apparentTemperature = c.apparentTemperature,
                    humidity = c.humidity,
                    dewPoint = hour?.dewPoint,
                    pressure = c.pressure,
                    cloudCover = c.cloudCover,
                    windSpeed = c.windSpeed,
                    windDirection = c.windDirection,
                    windGust = c.windGust,
                    uvIndex = hour?.uvIndex,
                    visibility = hour?.visibility,
                    precipitation = c.precipitation,
                    precipitationProbability = hour?.precipitationProbability,
                    condition = Conditions.fromWmo(c.weatherCode),
                    isDay = c.isDay?.let { it > 0 },
                )
            }
        val minutely =
            response.minutely?.let { m ->
                m.time.indices.mapNotNull { i ->
                    m.precipitation.getOrNull(i)?.let {
                        PrecipitationStep(Instant.ofEpochSecond(m.time[i]), it * 4)
                    }
                }
            } ?: emptyList()
        return ProviderForecast(Provider.OPEN_METEO, current, hours, days, minutely)
    }

    private fun List<Double?>.at(i: Int): Double? = getOrNull(i)
}

@Serializable
internal data class OmResponse(
    val timezone: String,
    val current: OmCurrent? = null,
    val hourly: OmHourly? = null,
    val daily: OmDaily? = null,
    @SerialName("minutely_15") val minutely: OmMinutely? = null,
)

@Serializable
internal data class OmMinutely(
    val time: List<Long> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
)

@Serializable
internal data class OmCurrent(
    val time: Long,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("cloud_cover") val cloudCover: Double? = null,
    @SerialName("pressure_msl") val pressure: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Double? = null,
    @SerialName("wind_gusts_10m") val windGust: Double? = null,
)

@Serializable
internal data class OmHourly(
    val time: List<Long> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val humidity: List<Double?> = emptyList(),
    @SerialName("dew_point_2m") val dewPoint: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability")
    val precipitationProbability: List<Double?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Double?> = emptyList(),
    @SerialName("pressure_msl") val pressure: List<Double?> = emptyList(),
    @SerialName("cloud_cover") val cloudCover: List<Double?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m") val windDirection: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m") val windGust: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
    @SerialName("is_day") val isDay: List<Double?> = emptyList(),
)

@Serializable
internal data class OmDaily(
    val time: List<Long> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Double?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    val sunrise: List<Double?> = emptyList(),
    val sunset: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max")
    val precipitationProbabilityMax: List<Double?> = emptyList(),
)

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.model

import java.time.Instant
import java.time.LocalDate

data class ProviderHour(
    val time: Instant,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val humidity: Double? = null,
    val dewPoint: Double? = null,
    val pressure: Double? = null,
    val cloudCover: Double? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null,
    val windGust: Double? = null,
    val uvIndex: Double? = null,
    val visibility: Double? = null,
    val precipitation: Double? = null,
    val precipitationProbability: Double? = null,
    val condition: Condition? = null,
    val isDay: Boolean? = null,
    val temperatureLow: Double? = null,
    val temperatureHigh: Double? = null,
    val windLow: Double? = null,
    val windHigh: Double? = null,
    val precipitationMin: Double? = null,
    val precipitationMax: Double? = null,
    val thunderProbability: Double? = null,
    val fog: Double? = null,
    val cloudLow: Double? = null,
    val cloudMid: Double? = null,
    val cloudHigh: Double? = null,
    val snowfall: Double? = null,
    val snowDepth: Double? = null,
)

data class ProviderDay(
    val date: LocalDate,
    val temperatureMax: Double? = null,
    val temperatureMin: Double? = null,
    val precipitationSum: Double? = null,
    val precipitationProbability: Double? = null,
    val condition: Condition? = null,
    val uvIndexMax: Double? = null,
    val sunrise: Instant? = null,
    val sunset: Instant? = null,
    val precipitationMin: Double? = null,
    val precipitationMax: Double? = null,
    val snowfallSum: Double? = null,
    val sunshineSeconds: Double? = null,
    val daylightSeconds: Double? = null,
)

data class ProviderForecast(
    val provider: Provider,
    val current: ProviderHour?,
    val hours: List<ProviderHour>,
    val days: List<ProviderDay>,
    val minutely: List<PrecipitationStep> = emptyList(),
)

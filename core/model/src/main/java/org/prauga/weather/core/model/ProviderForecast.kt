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
)

data class ProviderForecast(
    val provider: Provider,
    val current: ProviderHour?,
    val hours: List<ProviderHour>,
    val days: List<ProviderDay>,
)

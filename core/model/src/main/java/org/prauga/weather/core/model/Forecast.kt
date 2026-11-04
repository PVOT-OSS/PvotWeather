// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.model

import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

data class Sourced(val value: Double, val openMeteo: Double?, val metNorway: Double?) {
    val spread: Double?
        get() = if (openMeteo != null && metNorway != null) abs(openMeteo - metNorway) else null

    val providers: Set<Provider>
        get() = buildSet {
            if (openMeteo != null) add(Provider.OPEN_METEO)
            if (metNorway != null) add(Provider.MET_NORWAY)
        }
}

data class SourcedCondition(
    val value: Condition,
    val openMeteo: Condition?,
    val metNorway: Condition?,
) {
    val differs: Boolean
        get() = openMeteo != null && metNorway != null && openMeteo != metNorway

    val conflicts: Boolean
        get() = openMeteo != null && metNorway != null && openMeteo.isWet != metNorway.isWet
}

data class HourPoint(
    val time: Instant,
    val temperature: Sourced?,
    val apparentTemperature: Sourced?,
    val humidity: Sourced?,
    val dewPoint: Sourced?,
    val pressure: Sourced?,
    val cloudCover: Sourced?,
    val windSpeed: Sourced?,
    val windDirection: Sourced?,
    val windGust: Sourced?,
    val uvIndex: Sourced?,
    val visibility: Sourced?,
    val precipitation: Sourced?,
    val precipitationProbability: Sourced?,
    val condition: SourcedCondition?,
    val isDay: Boolean?,
)

data class DayPoint(
    val date: LocalDate,
    val temperatureMax: Sourced?,
    val temperatureMin: Sourced?,
    val precipitationSum: Sourced?,
    val precipitationProbability: Sourced?,
    val condition: SourcedCondition?,
    val uvIndexMax: Sourced?,
    val sunrise: Instant?,
    val sunset: Instant?,
)

data class Forecast(
    val place: Place,
    val current: HourPoint?,
    val hours: List<HourPoint>,
    val days: List<DayPoint>,
    val providers: Set<Provider>,
    val minutely: List<PrecipitationStep> = emptyList(),
)

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.model

import java.time.Instant
import java.time.LocalDate

data class PrecipitationStep(val time: Instant, val rate: Double)

data class Nowcast(val provider: Provider, val steps: List<PrecipitationStep>)

enum class AlertLevel {
    YELLOW,
    ORANGE,
    RED,
}

data class Alert(
    val id: String,
    val event: String,
    val level: AlertLevel,
    val area: String,
    val description: String,
    val instruction: String?,
    val ends: Instant?,
    val url: String?,
)

data class DailyHigh(val date: LocalDate, val temperatureMax: Double)

data class AirQuality(
    val time: Instant,
    val usAqi: Int,
    val pm25: Double?,
    val pm10: Double?,
    val europeanAqi: Int? = null,
    val ozone: Double? = null,
    val nitrogenDioxide: Double? = null,
    val sulphurDioxide: Double? = null,
    val carbonMonoxide: Double? = null,
    val pollen: Map<Pollen, Double> = emptyMap(),
)

enum class Pollen {
    ALDER,
    BIRCH,
    GRASS,
    MUGWORT,
    OLIVE,
    RAGWEED,
}

data class MetAirQuality(
    val time: Instant,
    val index: Double,
    val pm25: Double?,
    val pm10: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
    val area: String?,
)

data class Moon(val rise: Instant?, val set: Instant?, val phaseDegrees: Double)

data class SeaState(
    val waveHeight: Sourced?,
    val waveDirection: Double?,
    val wavePeriod: Double?,
    val seaTemperature: Sourced?,
    val currentSpeed: Double?,
    val currentDirection: Double?,
)

data class River(val date: LocalDate, val discharge: Double, val mean: Double?, val max: Double?)

data class EnsembleHour(val time: Instant, val low: Double, val high: Double, val members: Int)

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

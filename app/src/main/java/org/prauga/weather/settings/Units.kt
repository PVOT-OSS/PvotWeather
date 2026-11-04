// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import org.prauga.weather.R
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.core.model.WindUnit

data class Units(
    val temperature: TemperatureUnit = TemperatureUnit.CELSIUS,
    val wind: WindUnit = WindUnit.KILOMETRES_PER_HOUR,
)

val LocalUnits = staticCompositionLocalOf { Units() }

@get:StringRes
val TemperatureUnit.symbol: Int
    get() =
        when (this) {
            TemperatureUnit.CELSIUS -> R.string.unit_celsius
            TemperatureUnit.FAHRENHEIT -> R.string.unit_fahrenheit
        }

@get:StringRes
val TemperatureUnit.label: Int
    get() =
        when (this) {
            TemperatureUnit.CELSIUS -> R.string.unit_celsius_name
            TemperatureUnit.FAHRENHEIT -> R.string.unit_fahrenheit_name
        }

@get:StringRes
val WindUnit.symbol: Int
    get() =
        when (this) {
            WindUnit.KILOMETRES_PER_HOUR -> R.string.unit_kmh
            WindUnit.METRES_PER_SECOND -> R.string.unit_ms
            WindUnit.MILES_PER_HOUR -> R.string.unit_mph
            WindUnit.KNOTS -> R.string.unit_knots
        }

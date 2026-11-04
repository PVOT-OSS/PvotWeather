// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.model

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT;

    fun fromCelsius(value: Double): Double = if (this == CELSIUS) value else value * 9 / 5 + 32
}

enum class WindUnit {
    KILOMETRES_PER_HOUR,
    METRES_PER_SECOND,
    MILES_PER_HOUR,
    KNOTS;

    fun fromMetresPerSecond(value: Double): Double =
        when (this) {
            KILOMETRES_PER_HOUR -> value * 3.6
            METRES_PER_SECOND -> value
            MILES_PER_HOUR -> value * 2.236936
            KNOTS -> value * 1.943844
        }
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.model

// Declared from least to most severe: ordinal is the severity.
enum class Condition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN_SHOWERS,
    RAIN,
    SLEET,
    SNOW_SHOWERS,
    SNOW,
    HEAVY_RAIN,
    HEAVY_SNOW,
    THUNDERSTORM;

    val isWet: Boolean
        get() = this >= DRIZZLE

    companion object {
        fun mostSevere(a: Condition?, b: Condition?): Condition? =
            when {
                a == null -> b
                b == null -> a
                else -> maxOf(a, b)
            }
    }
}

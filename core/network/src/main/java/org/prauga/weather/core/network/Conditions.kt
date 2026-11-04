// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import org.prauga.weather.core.model.Condition

object Conditions {

    fun fromWmo(code: Int?): Condition? =
        when (code) {
            null -> null
            0,
            1 -> Condition.CLEAR
            2 -> Condition.PARTLY_CLOUDY
            3 -> Condition.CLOUDY
            45,
            48 -> Condition.FOG
            51,
            53,
            55,
            56,
            57 -> Condition.DRIZZLE
            61,
            63 -> Condition.RAIN
            65,
            82 -> Condition.HEAVY_RAIN
            66,
            67 -> Condition.SLEET
            71,
            73,
            77 -> Condition.SNOW
            75 -> Condition.HEAVY_SNOW
            80,
            81 -> Condition.RAIN_SHOWERS
            85,
            86 -> Condition.SNOW_SHOWERS
            95,
            96,
            99 -> Condition.THUNDERSTORM
            else -> null
        }

    fun fromMetSymbol(symbol: String?): Condition? {
        val base = symbol?.substringBefore('_') ?: return null
        val heavy = base.startsWith("heavy")
        val light = base.startsWith("light")
        val showers = "showers" in base
        return when {
            "thunder" in base -> Condition.THUNDERSTORM
            "sleet" in base -> Condition.SLEET
            "snow" in base ->
                when {
                    heavy -> Condition.HEAVY_SNOW
                    showers -> Condition.SNOW_SHOWERS
                    else -> Condition.SNOW
                }
            "rain" in base ->
                when {
                    heavy -> Condition.HEAVY_RAIN
                    showers -> Condition.RAIN_SHOWERS
                    light -> Condition.DRIZZLE
                    else -> Condition.RAIN
                }
            base == "fog" -> Condition.FOG
            base == "cloudy" -> Condition.CLOUDY
            base == "partlycloudy" -> Condition.PARTLY_CLOUDY
            base == "clearsky" || base == "fair" -> Condition.CLEAR
            else -> null
        }
    }

    fun isDayFromMetSymbol(symbol: String?): Boolean? =
        when (symbol?.substringAfter('_', "")) {
            "day" -> true
            "night",
            "polartwilight" -> false
            else -> null
        }
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class WeatherColors(
    val high: Color,
    val low: Color,
    val rain: Color,
    val openMeteo: Color,
    val metNorway: Color,
    val alertYellow: Color,
    val alertOrange: Color,
    val alertRed: Color,
    val uvScale: List<Color>,
    val aqiScale: List<Color>,
    val sun: Color,
    val sunPath: Color,
)

object WeatherColorsDefaults {
    val Colors =
        WeatherColors(
            high = Color(0xFFFF8A3D),
            low = Color(0xFF4FA3E0),
            rain = Color(0xFF4FA3E0),
            openMeteo = Color(0xFF26C6DA),
            metNorway = Color(0xFFB388FF),
            alertYellow = Color(0xFFF2C94C),
            alertOrange = Color(0xFFF2994A),
            alertRed = Color(0xFFEB5757),
            uvScale =
                listOf(
                    Color(0xFF3DBE5C),
                    Color(0xFFF2D43D),
                    Color(0xFFF28A2E),
                    Color(0xFFE5483B),
                    Color(0xFF9B4FD1),
                ),
            aqiScale =
                listOf(
                    Color(0xFF3DBE5C),
                    Color(0xFFF2D43D),
                    Color(0xFFF28A2E),
                    Color(0xFFE5483B),
                    Color(0xFF9B4FD1),
                    Color(0xFF7E2340),
                ),
            sun = Color(0xFFFFB300),
            sunPath = Color(0xFF8E8E93),
        )
}

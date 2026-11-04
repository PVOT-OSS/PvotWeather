// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import org.prauga.weather.core.model.Condition

enum class Backdrop(val colors: List<Color>) {
    CLEAR(listOf(Color(0xFFFF7300), Color(0xFFFFD580))),
    PARTLY_CLOUDY(listOf(Color(0xFFFEEAA1), Color(0xFFCFCFCF))),
    CLOUDY(listOf(Color(0xFFD7D7D7), Color(0xFF808080))),
    LIGHT_RAIN(listOf(Color(0xFFA0C9FF), Color(0xFF6F8FAF))),
    HEAVY_RAIN(listOf(Color(0xFF4A6FA5), Color(0xFF1C2B45))),
    THUNDERSTORM(listOf(Color(0xFF3A4C6B), Color(0xFF101820))),
    SNOW(listOf(Color(0xFFFFFFFF), Color(0xFFD8EFFF))),
    FOG(listOf(Color(0xFFE0E0E0), Color(0xFFB0B0B0))),
    SUNRISE_SUNSET(listOf(Color(0xFFFF9A8B), Color(0xFFFF6A88), Color(0xFFC64DFF))),
    NIGHT(listOf(Color(0xFF1B2735), Color(0xFF090A0F)));

    companion object {
        private val GOLDEN_HOUR: Duration = Duration.ofMinutes(45)

        fun of(
            condition: Condition?,
            isDay: Boolean?,
            now: Instant,
            sunrise: Instant?,
            sunset: Instant?,
        ): Backdrop {
            val dry =
                condition == null ||
                    condition == Condition.CLEAR ||
                    condition == Condition.PARTLY_CLOUDY
            val nearSunEvent =
                listOfNotNull(sunrise, sunset).any {
                    abs(Duration.between(it, now).toMinutes()) <= GOLDEN_HOUR.toMinutes()
                }
            return when {
                dry && nearSunEvent -> SUNRISE_SUNSET
                dry && isDay == false -> NIGHT
                else ->
                    when (condition) {
                        null,
                        Condition.CLEAR -> CLEAR
                        Condition.PARTLY_CLOUDY -> PARTLY_CLOUDY
                        Condition.CLOUDY -> CLOUDY
                        Condition.FOG -> FOG
                        Condition.DRIZZLE,
                        Condition.RAIN_SHOWERS,
                        Condition.RAIN,
                        Condition.SLEET -> LIGHT_RAIN
                        Condition.HEAVY_RAIN -> HEAVY_RAIN
                        Condition.SNOW_SHOWERS,
                        Condition.SNOW,
                        Condition.HEAVY_SNOW -> SNOW
                        Condition.THUNDERSTORM -> THUNDERSTORM
                    }
            }
        }
    }
}

@Composable
fun WeatherBackdrop(
    backdrop: Backdrop,
    background: Color,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val first by animateColorAsState(backdrop.colors[0], tween(800), label = "first")
    val second by animateColorAsState(backdrop.colors[1], tween(800), label = "second")
    val third by
        animateColorAsState(
            backdrop.colors.getOrElse(2) { backdrop.colors[1] },
            tween(800),
            label = "third",
        )
    val alpha = if (dark) 0.8f else 1f
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun blob(color: Color, x: Float, y: Float, radius: Float, strength: Float) =
            drawRect(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha * strength), color.copy(alpha = 0f)),
                    center = Offset(w * x, h * y),
                    radius = w * radius,
                )
            )
        blob(third, 0.15f, 0.55f, 0.8f, 0.7f)
        blob(second, 0.85f, 0.2f, 0.85f, 0.85f)
        blob(first, 0.35f, 0.25f, 0.9f, 1f)
        drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to background))
    }
}

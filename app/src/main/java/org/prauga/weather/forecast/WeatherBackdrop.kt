// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.prauga.weather.core.model.Condition

enum class Backdrop(val colors: List<Color>, val sky: Sky) {
    CLEAR(listOf(Color(0xFF3A8DDE), Color(0xFFC6E6FF)), Sky.SUN),
    PARTLY_CLOUDY(listOf(Color(0xFF7FB8EA), Color(0xFFC9CFD6)), Sky.CLOUDS),
    CLOUDY(listOf(Color(0xFFD7D7D7), Color(0xFF808080)), Sky.OVERCAST),
    LIGHT_RAIN(listOf(Color(0xFF8E9DAC), Color(0xFF5B6A7A)), Sky.RAIN),
    HEAVY_RAIN(listOf(Color(0xFF4A5868), Color(0xFF222B36)), Sky.DOWNPOUR),
    THUNDERSTORM(listOf(Color(0xFF2A3240), Color(0xFF07090E)), Sky.STORM),
    SNOW(listOf(Color(0xFFFFFFFF), Color(0xFFD8EFFF)), Sky.SNOW),
    FOG(listOf(Color(0xFFE0E0E0), Color(0xFFB0B0B0)), Sky.FOG),
    SUNRISE_SUNSET(listOf(Color(0xFFFF9A8B), Color(0xFFFF6A88), Color(0xFFC64DFF)), Sky.GLOW),
    NIGHT(listOf(Color(0xFF1B2735), Color(0xFF090A0F)), Sky.STARS);

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

private val SkyLight = Color.White
private val SkySun = Color(0xFFFFE9A8)
private val SkyShade = Color(0xFF1C2129)
private val SkyInkLight = Color(0xFF6F93B5)
private const val BLEND_MILLIS = 800

@Composable
fun WeatherBackdrop(
    backdrop: Backdrop,
    background: Color,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val first by animateColorAsState(backdrop.colors[0], tween(BLEND_MILLIS), label = "first")
    val second by animateColorAsState(backdrop.colors[1], tween(BLEND_MILLIS), label = "second")
    val third by
        animateColorAsState(
            backdrop.colors.getOrElse(2) { backdrop.colors[1] },
            tween(BLEND_MILLIS),
            label = "third",
        )
    val seconds = rememberSkyClock()
    var shown by remember { mutableStateOf(backdrop.sky) }
    var fading by remember { mutableStateOf<Sky?>(null) }
    val blend = remember { Animatable(1f) }
    LaunchedEffect(backdrop.sky) {
        if (backdrop.sky == shown) return@LaunchedEffect
        fading = shown
        shown = backdrop.sky
        blend.snapTo(0f)
        blend.animateTo(1f, tween(BLEND_MILLIS))
        fading = null
    }
    val path = remember { Path() }
    val alpha = if (dark) 0.8f else 1f
    val colors =
        remember(dark) {
            SkyColors(
                light = SkyLight,
                sun = SkySun,
                shade = SkyShade,
                ink = if (dark) SkyLight else SkyInkLight,
            )
        }
    Canvas(modifier) {
        val t = seconds.floatValue
        val w = size.width
        val h = size.height
        fun blob(color: Color, x: Float, y: Float, radius: Float, strength: Float, phase: Float) {
            val dx = sin(t * 0.09f + phase) * 0.035f
            val dy = cos(t * 0.07f + phase * 1.3f) * 0.03f
            val r = radius * (1f + 0.04f * sin(t * 0.11f + phase))
            drawRect(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha * strength), color.copy(alpha = 0f)),
                    center = Offset(w * (x + dx), h * (y + dy)),
                    radius = w * r,
                )
            )
        }
        blob(third, 0.15f, 0.55f, 0.8f, 0.7f, 0f)
        blob(second, 0.85f, 0.2f, 0.85f, 0.85f, 2.1f)
        blob(first, 0.35f, 0.25f, 0.9f, 1f, 4.2f)
        fading?.let { drawSky(it, t, alpha * (1f - blend.value), colors, path) }
        drawSky(shown, t, alpha * blend.value, colors, path)
        drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to background))
    }
}

@Composable
private fun rememberSkyClock(): androidx.compose.runtime.FloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { seconds.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    return seconds
}

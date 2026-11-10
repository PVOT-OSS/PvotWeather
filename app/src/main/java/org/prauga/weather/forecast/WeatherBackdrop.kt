// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
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
    NIGHT_CLOUDY(listOf(Color(0xFF3B4352), Color(0xFF1C2028)), Sky.OVERCAST),
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
                        Condition.CLOUDY -> if (isDay == false) NIGHT_CLOUDY else CLOUDY
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

/** A calm sky's frame interval: 30 frames a second. */
private const val CALM_FRAME_NANOS = 33_000_000L

/** How much of the sky the dark theme lets through; the rest is the theme background. */
private const val DARK_SKY_ALPHA = 0.8f

/** Text and icons drawn straight on a light sky. */
private val SkyInkDark = Color(0xFF1A1C1E)

/** The luminance that contrasts equally with black and with white (WCAG 2). */
private const val MID_LUMINANCE = 0.18f

/**
 * Whether the top of this sky, drawn by [WeatherBackdrop] over [background], is light enough that
 * dark status-bar icons read better on it than light ones.
 *
 * Approximates what the canvas paints under the status bar: the first colour, washed over the top
 * and lifted by the first blob, dimmed in the dark theme like the canvas dims it.
 */
fun Backdrop.isLight(background: Color, dark: Boolean): Boolean =
    top(background, dark).luminance() > MID_LUMINANCE

/** The colour the canvas paints at the top of this sky, under the status bar. */
fun Backdrop.top(background: Color, dark: Boolean): Color =
    colors[0].copy(alpha = if (dark) DARK_SKY_ALPHA else 1f).compositeOver(background)

/**
 * The colour for text and icons drawn straight on this sky: white or near-black, whichever
 * contrasts more with the sky's middle, where the place, temperature and hourly strip sit.
 */
fun Backdrop.onSky(background: Color, dark: Boolean): Color {
    val alpha = if (dark) DARK_SKY_ALPHA else 1f
    val middle = lerp(colors[0], colors[1], 0.5f).copy(alpha = alpha).compositeOver(background)
    val sky = middle.luminance()
    val onWhite = 1.05f / (sky + 0.05f)
    val onInk = (sky + 0.05f) / (SkyInkDark.luminance() + 0.05f)
    return if (onWhite >= onInk) Color.White else SkyInkDark
}

@Composable
fun WeatherBackdrop(
    backdrop: Backdrop,
    dark: Boolean,
    modifier: Modifier = Modifier,
    running: Boolean = true,
) {
    val first by animateColorAsState(backdrop.colors[0], tween(BLEND_MILLIS), label = "first")
    val second by animateColorAsState(backdrop.colors[1], tween(BLEND_MILLIS), label = "second")
    val third by
        animateColorAsState(
            backdrop.colors.getOrElse(2) { backdrop.colors[1] },
            tween(BLEND_MILLIS),
            label = "third",
        )
    val seconds = rememberSkyClock(running, backdrop.sky.calm)
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
    val alpha = if (dark) DARK_SKY_ALPHA else 1f
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
        // The palette painted over the whole canvas first, so the theme background never shows
        // through between the blobs as they drift.
        drawRect(
            Brush.verticalGradient(
                0f to first.copy(alpha = alpha),
                0.5f to second.copy(alpha = alpha),
                1f to third.copy(alpha = alpha),
            )
        )
        blob(third, 0.15f, 0.55f, 0.8f, 0.7f, 0f)
        blob(second, 0.85f, 0.2f, 0.85f, 0.85f, 2.1f)
        blob(first, 0.35f, 0.25f, 0.9f, 1f, 4.2f)
        fading?.let { drawSky(it, t, alpha * (1f - blend.value), colors, path) }
        drawSky(shown, t, alpha * blend.value, colors, path)
    }
}

/**
 * Elapsed seconds while [running], ticking every frame, or at [CALM_FRAME_NANOS] for a [calm] sky
 * whose slow drift needs no more, which spares the GPU most of its frames.
 */
@Composable
private fun rememberSkyClock(running: Boolean, calm: Boolean): FloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animatorsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val animate = running && animatorsEnabled && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val base = seconds.floatValue
        val start = withFrameNanos { it }
        var shown = start
        while (true) {
            withFrameNanos {
                if (!calm || it - shown >= CALM_FRAME_NANOS) {
                    shown = it
                    seconds.floatValue = base + (it - start) / 1_000_000_000f
                }
            }
        }
    }
    return seconds
}

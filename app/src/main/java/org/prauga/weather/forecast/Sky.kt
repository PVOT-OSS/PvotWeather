// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

enum class Sky {
    SUN,
    GLOW,
    CLOUDS,
    OVERCAST,
    RAIN,
    DOWNPOUR,
    STORM,
    SNOW,
    FOG,
    STARS,
}

class SkyColors(val light: Color, val sun: Color, val shade: Color, val ink: Color)

/**
 * Every sky is a pure function of elapsed seconds, so frames read no state and allocate nothing.
 */
object SkyMath {
    const val FLASH_PERIOD = 4.2f
    const val METEOR_PERIOD = 9f
    private const val FLASH_SPAN = 0.18f
    private const val ECHO_DELAY = 0.22f
    private const val ECHO_SPAN = 0.3f
    private const val METEOR_SPAN = 0.9f

    fun noise(i: Int, salt: Int = 0): Float {
        val x = sin(i * 12.9898f + salt * 78.233f) * 43758.5453f
        return x - floor(x)
    }

    fun flashCycle(t: Float): Int = floor(t / FLASH_PERIOD).toInt()

    fun flash(t: Float): Float {
        val cycle = flashCycle(t)
        if (noise(cycle, 7) > 0.65f) return 0f
        val local = t - cycle * FLASH_PERIOD - noise(cycle, 8) * (FLASH_PERIOD - 0.8f)
        val strike = decay(local, FLASH_SPAN)
        val echo = decay(local - ECHO_DELAY, ECHO_SPAN) * 0.6f
        return maxOf(strike, echo)
    }

    fun meteorCycle(t: Float): Int = floor(t / METEOR_PERIOD).toInt()

    fun meteor(t: Float): Float {
        val cycle = meteorCycle(t)
        val local = t - cycle * METEOR_PERIOD - noise(cycle, 21) * (METEOR_PERIOD - 1.5f)
        return if (local in 0f..METEOR_SPAN) local / METEOR_SPAN else -1f
    }

    private fun decay(dt: Float, span: Float) = if (dt < 0f || dt > span) 0f else 1f - dt / span
}

private const val TAU = (2 * PI).toFloat()

fun DrawScope.drawSky(sky: Sky, t: Float, alpha: Float, colors: SkyColors, path: Path) {
    if (alpha <= 0.001f) return
    when (sky) {
        Sky.SUN -> sun(t, alpha, colors.sun, path)
        Sky.GLOW -> glow(t, alpha, colors.sun)
        Sky.CLOUDS -> cloudDeck(t, alpha, colors, perLayer = 3, strength = 0.5f, cover = 0.5f)
        Sky.OVERCAST -> cloudDeck(t, alpha, colors, perLayer = 7, strength = 0.42f, cover = 0.75f)
        Sky.RAIN -> {
            rainClouds(t, alpha, colors.shade, strength = 0.28f)
            rainLayers(t, alpha, colors.light, intensity = 1f, lean = 7f)
        }
        Sky.DOWNPOUR -> {
            drawRect(colors.shade.copy(alpha = alpha * 0.15f))
            rainClouds(t, alpha, colors.shade, strength = 0.4f)
            rainLayers(t, alpha, colors.light, intensity = 2.2f, lean = 12f + 4f * sin(t * 0.4f))
        }
        Sky.STORM -> {
            drawRect(colors.shade.copy(alpha = alpha * 0.4f))
            rainClouds(t, alpha, colors.shade, strength = 0.55f)
            rainLayers(t, alpha, colors.light, intensity = 2.4f, lean = 14f + 6f * sin(t * 0.5f))
            lightning(t, alpha, colors.light, path)
        }
        Sky.SNOW -> snow(t, alpha, colors.ink)
        Sky.FOG -> fog(t, alpha, colors.ink)
        Sky.STARS -> stars(t, alpha, colors.light)
    }
}

private fun DrawScope.puff(color: Color, x: Float, y: Float, radius: Float, alpha: Float) {
    if (alpha <= 0f) return
    drawCircle(
        Brush.radialGradient(
            listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = Offset(x, y),
            radius = radius,
        ),
        radius = radius,
        center = Offset(x, y),
    )
}

private fun DrawScope.sun(t: Float, alpha: Float, light: Color, path: Path) {
    val w = size.width
    val cx = w * 0.3f
    val cy = size.height * 0.2f
    val pulse = 0.5f + 0.5f * sin(t * 0.6f)
    puff(light, cx, cy, w * (0.26f + 0.03f * pulse), alpha * (0.5f + 0.15f * pulse))
    val rays = 7
    val length = w * 1.3f
    for (i in 0 until rays) {
        val angle = t * 0.06f + i * (TAU / rays) + SkyMath.noise(i, 3) * 0.6f
        val half = 0.05f + 0.05f * SkyMath.noise(i, 4)
        path.reset()
        path.moveTo(cx, cy)
        path.lineTo(cx + cos(angle - half) * length, cy + sin(angle - half) * length)
        path.lineTo(cx + cos(angle + half) * length, cy + sin(angle + half) * length)
        path.close()
        val shimmer = 0.6f + 0.4f * sin(t * 0.9f + i * 1.7f)
        drawPath(
            path,
            Brush.radialGradient(
                listOf(light.copy(alpha = alpha * 0.16f * shimmer), light.copy(alpha = 0f)),
                center = Offset(cx, cy),
                radius = length * 0.75f,
            ),
        )
    }
}

private fun DrawScope.glow(t: Float, alpha: Float, light: Color) {
    val w = size.width
    val h = size.height
    val pulse = 0.5f + 0.5f * sin(t * 0.5f)
    puff(light, w * 0.5f, h * 0.6f, w * (0.3f + 0.03f * pulse), alpha * (0.5f + 0.1f * pulse))
    for (i in 0 until 3) {
        val speed = h * (0.02f + 0.015f * SkyMath.noise(i, 5))
        val y = h - ((SkyMath.noise(i, 6) * h + t * speed) % h)
        val x = w * (0.2f + 0.6f * SkyMath.noise(i, 7)) + sin(t * 0.2f + i) * w * 0.08f
        puff(light, x, y, w * 0.35f, alpha * 0.16f)
    }
}

private fun DrawScope.clouds(
    t: Float,
    alpha: Float,
    color: Color,
    count: Int,
    scale: Float,
    strength: Float,
    band: Float,
) {
    val w = size.width
    val h = size.height
    for (i in 0 until count) {
        val r = w * scale * (0.8f + 0.5f * SkyMath.noise(i, 10))
        val speed = w * (0.012f + 0.014f * SkyMath.noise(i, 11))
        val x = ((SkyMath.noise(i, 12) * w + t * speed) % (w + 3 * r)) - 1.5f * r
        val y = h * (0.06f + band * SkyMath.noise(i, 13)) + sin(t * 0.3f + i * 2.1f) * r * 0.06f
        val a = alpha * strength * (0.7f + 0.3f * SkyMath.noise(i, 14))
        puff(color, x, y, r, a)
        puff(color, x + r * 0.75f, y - r * 0.25f, r * 0.8f, a)
        puff(color, x - r * 0.7f, y - r * 0.1f, r * 0.7f, a * 0.9f)
    }
}

/** How much wider than tall a wisp is, by layer from far to near. */
private val DeckStretch = floatArrayOf(4.2f, 3.2f, 2.4f)

/** A wisp's height, as a fraction of the sky's, by layer from far to near. */
private val DeckSize = floatArrayOf(0.05f, 0.075f, 0.1f)

/** How fast a layer drifts, in sky widths a second, from far to near. */
private val DeckSpeed = floatArrayOf(0.006f, 0.012f, 0.022f)

/**
 * A deck of cloud in three layers, [perLayer] wisps each: far wisps long, thin, faint and slow,
 * near ones rounder, stronger and faster, so the sky drifts with some depth. Each wisp is a lit
 * body over a darker underside, thickest near the top of the sky and thinning out by [cover] of its
 * height.
 */
private fun DrawScope.cloudDeck(
    t: Float,
    alpha: Float,
    colors: SkyColors,
    perLayer: Int,
    strength: Float,
    cover: Float,
) {
    val w = size.width
    val h = size.height
    for (layer in 0 until 3) {
        val depth = (layer + 1) / 3f
        for (i in 0 until perLayer) {
            val id = layer * 29 + i
            val r = h * DeckSize[layer] * (0.75f + 0.5f * SkyMath.noise(id, 40))
            val stretch = DeckStretch[layer] * (0.8f + 0.4f * SkyMath.noise(id, 41))
            val speed = w * DeckSpeed[layer] * (0.8f + 0.4f * SkyMath.noise(id, 42))
            val span = w + 2 * stretch * r
            val x = (SkyMath.noise(id, 43) * span + t * speed) % span - stretch * r
            val y =
                h * cover * SkyMath.noise(id, 44).pow(1.4f) + sin(t * 0.15f + id * 1.9f) * r * 0.08f
            val a =
                alpha * strength * (0.45f + 0.55f * depth) * (0.6f + 0.4f * SkyMath.noise(id, 45))
            val breathe = 1f + 0.07f * sin(t * 0.3f + id * 2.3f)
            wisp(colors.shade, x, y + r * 0.35f, r * breathe, stretch, a * 0.5f, core = 0.25f)
            wisp(colors.light, x, y, r * 0.85f * breathe, stretch, a * 0.72f, core = 0.3f)
        }
    }
}

/**
 * A soft puff [stretch] times wider than tall: solid out to [core] of its radius, then fading to
 * nothing at its edge.
 */
private fun DrawScope.wisp(
    color: Color,
    x: Float,
    y: Float,
    radius: Float,
    stretch: Float,
    alpha: Float,
    core: Float,
) {
    if (alpha <= 0f) return
    val centre = Offset(x, y)
    withTransform({ scale(stretch, 1f, pivot = centre) }) {
        drawCircle(
            Brush.radialGradient(
                0f to color.copy(alpha = alpha),
                core to color.copy(alpha = alpha),
                1f to color.copy(alpha = 0f),
                center = centre,
                radius = radius,
            ),
            radius = radius,
            center = centre,
        )
    }
}

private fun DrawScope.rainClouds(t: Float, alpha: Float, shade: Color, strength: Float) {
    clouds(t, alpha, shade, count = 7, scale = 0.24f, strength = strength, band = 0.22f)
}

private fun DrawScope.rainLayers(
    t: Float,
    alpha: Float,
    light: Color,
    intensity: Float,
    lean: Float,
) {
    rain(
        t,
        alpha * 0.55f,
        light,
        count = (70 * intensity).toInt(),
        speed = 1.4f,
        length = 0.028f,
        lean = lean,
        width = 0.9f,
        salt = 0,
    )
    rain(
        t,
        alpha,
        light,
        count = (45 * intensity).toInt(),
        speed = 2.4f,
        length = 0.07f,
        lean = lean * 1.25f,
        width = 1.7f,
        salt = 500,
    )
}

private fun DrawScope.rain(
    t: Float,
    alpha: Float,
    ink: Color,
    count: Int,
    speed: Float,
    length: Float,
    lean: Float,
    width: Float,
    salt: Int,
) {
    val w = size.width
    val h = size.height
    val angle = lean * TAU / 360f
    val tilt = tan(angle)
    val stroke = width * density
    for (k in 0 until count) {
        val i = k + salt
        val x0 = SkyMath.noise(i, 1) * w
        val y0 = SkyMath.noise(i, 2) * h
        val l = h * length * (0.7f + 0.6f * SkyMath.noise(i, 4))
        val y = ((y0 + t * h * speed * (0.85f + 0.4f * SkyMath.noise(i, 3))) % (h + l)) - l
        val x = ((x0 - (y - y0) * tilt) % w + w) % w
        val a = alpha * (0.3f + 0.35f * SkyMath.noise(i, 5))
        drawLine(
            ink.copy(alpha = a),
            Offset(x, y),
            Offset(x + sin(angle) * l, y - cos(angle) * l),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.lightning(t: Float, alpha: Float, light: Color, path: Path) {
    val flash = SkyMath.flash(t)
    if (flash <= 0f) return
    drawRect(light.copy(alpha = alpha * 0.45f * flash))
    if (flash < 0.5f) return
    val w = size.width
    val h = size.height
    val cycle = SkyMath.flashCycle(t)
    var x = w * (0.2f + 0.6f * SkyMath.noise(cycle, 11))
    var y = 0f
    path.reset()
    path.moveTo(x, y)
    val segments = 7
    for (s in 1..segments) {
        x += (SkyMath.noise(cycle * 31 + s, 12) - 0.5f) * w * 0.18f
        y += h * 0.55f / segments * (0.6f + 0.8f * SkyMath.noise(cycle * 31 + s, 13))
        path.lineTo(x, y)
    }
    val bolt = alpha * (flash - 0.5f) * 2f
    drawPath(
        path,
        light.copy(alpha = bolt * 0.35f),
        style = Stroke(width = 7f * density, cap = StrokeCap.Round),
    )
    drawPath(
        path,
        light.copy(alpha = bolt),
        style = Stroke(width = 2f * density, cap = StrokeCap.Round),
    )
}

private fun DrawScope.snow(t: Float, alpha: Float, ink: Color) {
    val w = size.width
    val h = size.height
    for (i in 0 until 80) {
        val depth = SkyMath.noise(i, 4)
        val r = (1.2f + 2.4f * depth) * density
        val speed = (28f + 55f * depth) * density
        val y = ((SkyMath.noise(i, 2) * h + t * speed) % (h + 2 * r)) - r
        val sway = sin(t * (0.5f + 0.5f * SkyMath.noise(i, 6)) + SkyMath.noise(i, 7) * TAU)
        val x = ((SkyMath.noise(i, 1) * w + sway * 14f * density) % w + w) % w
        drawCircle(ink.copy(alpha = alpha * (0.45f + 0.5f * depth)), r, Offset(x, y))
    }
}

private fun DrawScope.fog(t: Float, alpha: Float, ink: Color) {
    val w = size.width
    val h = size.height
    for (i in 0 until 5) {
        val r = h * 0.13f
        val direction = if (i % 2 == 0) 1f else -1f
        val speed = direction * (10f + 12f * SkyMath.noise(i, 8)) * density
        val span = w + 6 * r
        val x = (((SkyMath.noise(i, 9) * span + t * speed) % span) + span) % span - 3 * r
        val y = h * (0.1f + 0.15f * i)
        val a = alpha * (0.16f + 0.08f * sin(t * 0.25f + i * 1.3f))
        for (k in 0 until 4) puff(ink, x + k * r * 1.1f, y, r, a)
    }
}

private fun DrawScope.stars(t: Float, alpha: Float, light: Color) {
    val w = size.width
    val h = size.height
    for (i in 0 until 90) {
        val size = SkyMath.noise(i, 4)
        val r = (0.6f + 1.4f * size) * density
        val twinkle =
            0.35f +
                0.65f *
                    abs(sin(t * (0.6f + 1.4f * SkyMath.noise(i, 5)) + SkyMath.noise(i, 6) * TAU))
        val x = SkyMath.noise(i, 1) * w
        val y = SkyMath.noise(i, 2) * h * 0.7f
        drawCircle(light.copy(alpha = alpha * 0.9f * twinkle), r, Offset(x, y))
    }
    val progress = SkyMath.meteor(t)
    if (progress < 0f) return
    val cycle = SkyMath.meteorCycle(t)
    val start =
        Offset(w * (0.1f + 0.6f * SkyMath.noise(cycle, 22)), h * 0.1f * SkyMath.noise(cycle, 23))
    val travel = Offset(w * 0.45f, h * 0.3f)
    val head = start + travel * progress
    val tail = head - Offset(60f * density, 40f * density)
    drawLine(
        Brush.linearGradient(
            listOf(light.copy(alpha = 0f), light.copy(alpha = alpha * (1f - progress))),
            tail,
            head,
        ),
        tail,
        head,
        strokeWidth = 1.5f * density,
        cap = StrokeCap.Round,
    )
}

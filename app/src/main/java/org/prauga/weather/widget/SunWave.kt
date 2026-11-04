// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import org.prauga.weather.theme.WeatherColorsDefaults

object SunWave {
    fun render(
        context: Context,
        widthDp: Int,
        heightDp: Int,
        sunrise: Instant,
        sunset: Instant,
        now: Instant,
        zone: ZoneId,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = max(1, (widthDp * density).roundToInt())
        val h = max(1, (heightDp * density).roundToInt())
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        val ink = WeatherColorsDefaults.Colors.sunPath.toArgb()

        val midnight = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        fun hours(t: Instant) = Duration.between(midnight, t).toMinutes() / 60.0
        val rise = hours(sunrise)
        val set = hours(sunset)
        val noon = (rise + set) / 2
        val horizon = cos(2 * PI * (rise - noon) / 24)
        fun f(t: Double) = cos(2 * PI * (t - noon) / 24) - horizon
        val peak = max(f(noon), 0.01)
        val trough = max(-f(noon + 12), 0.01)
        val pad = 6 * density
        val horizonY = pad + (h - 2 * pad) * (peak / (peak + trough)).toFloat()
        fun x(t: Double) = (w * t / 24).toFloat()
        fun y(t: Double): Float {
            val v = f(t)
            return if (v >= 0) horizonY - ((horizonY - pad) * v / peak).toFloat()
            else horizonY + ((h - pad - horizonY) * -v / trough).toFloat()
        }

        val line =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ink
                alpha = 140
                strokeWidth = 1 * density
                pathEffect = DashPathEffect(floatArrayOf(4 * density, 4 * density), 0f)
            }
        canvas.drawLine(0f, horizonY, w.toFloat(), horizonY, line)

        val curve = Path()
        var t = 0.0
        curve.moveTo(x(t), y(t))
        while (t < 24) {
            t += 0.1
            curve.lineTo(x(t), y(t))
        }
        val stroke =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ink
                alpha = 255
                style = Paint.Style.STROKE
                strokeWidth = 2 * density
                strokeCap = Paint.Cap.ROUND
            }
        canvas.drawPath(curve, stroke)

        val nowHours = hours(now).coerceIn(0.0, 24.0)
        val sun =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (f(nowHours) >= 0) WeatherColorsDefaults.Colors.sun.toArgb() else ink
            }
        canvas.drawCircle(x(nowHours), y(nowHours), 6 * density, sun)
        return bitmap
    }
}

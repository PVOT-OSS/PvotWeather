// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.cos
import kotlin.math.sin
import org.prauga.weather.core.model.Condition

@Composable
fun ConditionIcon(
    condition: Condition?,
    isDay: Boolean?,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val night = isDay == false
        when (condition) {
            null,
            Condition.CLEAR ->
                if (night) moon(Offset(s / 2, s / 2), s * 0.26f, color, stroke)
                else sun(Offset(s / 2, s / 2), s * 0.18f, color, stroke)
            Condition.PARTLY_CLOUDY -> {
                val cloud = cloud(s * 0.24f, s * 0.42f, s * 0.68f)
                clipPath(cloud, ClipOp.Difference) {
                    if (night) moon(Offset(s * 0.36f, s * 0.36f), s * 0.2f, color, stroke)
                    else sun(Offset(s * 0.36f, s * 0.36f), s * 0.13f, color, stroke)
                }
                drawPath(cloud, color, style = stroke)
            }
            Condition.CLOUDY ->
                drawPath(cloud(s * 0.1f, s * 0.24f, s * 0.8f), color, style = stroke)
            Condition.FOG -> {
                listOf(0.3f to 0.75f, 0.45f to 0.6f, 0.6f to 0.8f, 0.75f to 0.5f).forEachIndexed {
                    i,
                    (y, len) ->
                    val x0 = s * (if (i % 2 == 0) 0.12f else 0.22f)
                    drawLine(
                        color,
                        Offset(x0, s * y),
                        Offset(x0 + s * len * 0.85f, s * y),
                        stroke.width,
                        StrokeCap.Round,
                    )
                }
            }
            else -> {
                drawPath(cloud(s * 0.12f, s * 0.08f, s * 0.76f), color, style = stroke)
                precipitation(condition, s, color, stroke)
            }
        }
    }
}

private fun DrawScope.precipitation(condition: Condition, s: Float, color: Color, stroke: Stroke) {
    fun drop(x: Float, y: Float, length: Float = 0.16f) =
        drawLine(
            color,
            Offset(s * x, s * y),
            Offset(s * (x - 0.05f), s * (y + length)),
            stroke.width,
            StrokeCap.Round,
        )
    fun flake(x: Float, y: Float) = drawCircle(color, s * 0.045f, Offset(s * x, s * y))
    when (condition) {
        Condition.DRIZZLE -> listOf(0.34f, 0.52f, 0.7f).forEach { drop(it, 0.66f, 0.08f) }
        Condition.RAIN_SHOWERS,
        Condition.RAIN -> listOf(0.38f, 0.56f, 0.74f).forEach { drop(it, 0.64f) }
        Condition.HEAVY_RAIN -> {
            listOf(0.32f, 0.5f, 0.68f, 0.86f).forEach { drop(it, 0.62f, 0.24f) }
        }
        Condition.SLEET -> {
            drop(0.4f, 0.64f)
            flake(0.62f, 0.72f)
        }
        Condition.SNOW_SHOWERS,
        Condition.SNOW ->
            listOf(0.36f to 0.7f, 0.56f to 0.82f, 0.72f to 0.68f).forEach { (x, y) -> flake(x, y) }
        Condition.HEAVY_SNOW ->
            listOf(0.3f to 0.68f, 0.48f to 0.8f, 0.64f to 0.68f, 0.8f to 0.8f, 0.4f to 0.92f)
                .forEach { (x, y) -> flake(x, y) }
        Condition.THUNDERSTORM -> {
            val bolt =
                Path().apply {
                    moveTo(s * 0.56f, s * 0.6f)
                    lineTo(s * 0.44f, s * 0.78f)
                    lineTo(s * 0.58f, s * 0.78f)
                    lineTo(s * 0.46f, s * 0.96f)
                }
            drawPath(bolt, color, style = stroke)
        }
        else -> Unit
    }
}

private fun cloud(left: Float, top: Float, width: Float): Path {
    fun circle(cx: Float, cy: Float, r: Float) =
        Path().apply { addOval(Rect(Offset(left + cx * width, top + cy * width), r * width)) }
    val base =
        Path().apply {
            addRect(
                Rect(
                    left + 0.25f * width,
                    top + 0.3f * width,
                    left + 0.75f * width,
                    top + 0.59f * width,
                )
            )
        }
    return listOf(
            circle(0.25f, 0.37f, 0.22f),
            circle(0.5f, 0.25f, 0.25f),
            circle(0.75f, 0.37f, 0.22f),
        )
        .fold(base) { acc, p -> Path.combine(PathOperation.Union, acc, p) }
}

private fun DrawScope.sun(center: Offset, r: Float, color: Color, stroke: Stroke) {
    drawCircle(color, r, center, style = stroke)
    repeat(8) { i ->
        val a = Math.toRadians(i * 45.0)
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        drawLine(
            color,
            Offset(center.x + dx * r * 1.5f, center.y + dy * r * 1.5f),
            Offset(center.x + dx * r * 2.0f, center.y + dy * r * 2.0f),
            stroke.width,
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.moon(center: Offset, r: Float, color: Color, stroke: Stroke) {
    val disc = Path().apply { addOval(Rect(center, r)) }
    val bite =
        Path().apply { addOval(Rect(Offset(center.x + r * 0.55f, center.y - r * 0.4f), r * 0.85f)) }
    drawPath(Path.combine(PathOperation.Difference, disc, bite), color, style = stroke)
}

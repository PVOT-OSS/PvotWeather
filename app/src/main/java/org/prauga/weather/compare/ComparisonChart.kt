// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class Series(val color: Color, val values: List<Double?>)

data class Band(val color: Color, val low: List<Double?>, val high: List<Double?>)

@Composable
fun ComparisonChart(
    series: List<Series>,
    axisLabels: List<String>,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    bands: List<Band> = emptyList(),
) {
    val all =
        series.flatMap { it.values.filterNotNull() } +
            bands.flatMap { it.low.filterNotNull() + it.high.filterNotNull() }
    if (all.isEmpty()) return
    val min = all.min()
    val max = all.max().let { if (it - min < 1) min + 1 else it }
    val grid = MaterialTheme.colorScheme.outlineVariant
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                format(max),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
        }
        Canvas(Modifier.fillMaxWidth().height(120.dp).padding(vertical = 4.dp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            listOf(0f, size.height / 2, size.height).forEach { y ->
                drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = dash)
            }
            bands.forEach { band ->
                val count = band.low.size
                if (count < 2) return@forEach
                fun x(i: Int) = size.width * i / (count - 1)
                fun y(v: Double) = size.height * (1 - ((v - min) / (max - min))).toFloat()
                for (i in 0 until count - 1) {
                    val l0 = band.low[i] ?: continue
                    val h0 = band.high.getOrNull(i) ?: continue
                    val l1 = band.low[i + 1] ?: continue
                    val h1 = band.high.getOrNull(i + 1) ?: continue
                    val quad =
                        Path().apply {
                            moveTo(x(i), y(h0))
                            lineTo(x(i + 1), y(h1))
                            lineTo(x(i + 1), y(l1))
                            lineTo(x(i), y(l0))
                            close()
                        }
                    drawPath(quad, band.color)
                }
            }
            series.forEach { s ->
                val count = s.values.size
                if (count < 2) return@forEach
                val path = Path()
                var drawing = false
                s.values.forEachIndexed { i, v ->
                    if (v == null) {
                        drawing = false
                        return@forEachIndexed
                    }
                    val x = size.width * i / (count - 1)
                    val y = size.height * (1 - ((v - min) / (max - min))).toFloat()
                    if (drawing) path.lineTo(x, y) else path.moveTo(x, y)
                    drawing = true
                }
                drawPath(path, s.color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                format(min),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            axisLabels.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

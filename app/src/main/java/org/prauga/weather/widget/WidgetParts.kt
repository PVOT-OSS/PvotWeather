// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import org.prauga.weather.MainActivity

@Composable
fun WidgetFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        content = content,
    )
}

@Composable
fun Label(
    text: String,
    size: TextUnit = 13.sp,
    muted: Boolean = false,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text,
        style =
            TextStyle(
                color =
                    if (muted) GlanceTheme.colors.onSurfaceVariant
                    else GlanceTheme.colors.onSurface,
                fontSize = size,
                textAlign = align,
            ),
        maxLines = 1,
    )
}

@Composable
fun Icon(@DrawableRes icon: Int, size: Dp, muted: Boolean = false) {
    Image(
        ImageProvider(icon),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter =
            ColorFilter.tint(
                if (muted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface
            ),
    )
}

@Composable
fun NowHeader(data: WidgetData, wide: Boolean) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(GlanceModifier.defaultWeight()) {
            Label(data.place)
            data.temperature?.let {
                Text(
                    it,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                )
            }
        }
        if (wide) {
            Column(horizontalAlignment = Alignment.End) {
                data.icon?.let { Icon(it, 28.dp) }
                data.condition?.let { Label(it, muted = true) }
                data.highLow?.let { Label(it, muted = true) }
            }
        }
    }
    if (!wide) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            data.icon?.let {
                Icon(it, 16.dp, muted = true)
                Spacer(GlanceModifier.width(4.dp))
            }
            data.condition?.let { Label(it, muted = true) }
        }
        data.highLow?.let { Label(it, muted = true) }
    }
}

@Composable
fun HourRow(hours: List<WidgetHour>) {
    Row(GlanceModifier.fillMaxWidth()) {
        hours.forEach { hour ->
            Column(
                GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Label(hour.label, size = 12.sp, muted = true, align = TextAlign.Center)
                Spacer(GlanceModifier.height(4.dp))
                Icon(hour.icon, 22.dp)
                Spacer(GlanceModifier.height(4.dp))
                Label(hour.temperature, size = 14.sp, align = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DayRows(days: List<WidgetDay>) {
    days.forEach { day ->
        Row(
            GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) { Label(day.label, size = 14.sp) }
            Icon(day.icon, 20.dp)
            Column(GlanceModifier.width(48.dp).padding(start = 6.dp)) {
                Label(day.rain ?: "", size = 12.sp, muted = true)
            }
            Column(GlanceModifier.width(44.dp)) {
                Label(day.low, size = 14.sp, muted = true, align = TextAlign.End)
            }
            Column(GlanceModifier.width(44.dp)) {
                Label(day.high, size = 14.sp, align = TextAlign.End)
            }
        }
    }
}

@Composable
fun DetailGrid(details: List<WidgetDetail>) {
    details.chunked(2).forEach { row ->
        Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
            row.forEach { detail ->
                Row(
                    GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(detail.icon, 18.dp, muted = true)
                    Spacer(GlanceModifier.width(6.dp))
                    Column {
                        Label(detail.label, size = 11.sp, muted = true)
                        Label(detail.value, size = 14.sp)
                    }
                }
            }
        }
    }
}

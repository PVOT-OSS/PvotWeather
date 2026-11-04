// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.prauga.weather.MainActivity

class WeatherWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        provideContent { GlanceTheme { Content(data) } }
    }

    @Composable
    private fun Content(data: WidgetData) {
        val colors = GlanceTheme.colors
        val wide = LocalSize.current.width >= WIDE.width
        Row(
            GlanceModifier.fillMaxSize()
                .background(colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    data.place,
                    style = TextStyle(color = colors.onSurface, fontSize = 13.sp),
                    maxLines = 1,
                )
                data.temperature?.let {
                    Text(
                        it,
                        style =
                            TextStyle(
                                color = colors.onSurface,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                    )
                }
                data.condition?.let {
                    Text(
                        it,
                        style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp),
                        maxLines = 1,
                    )
                }
                data.highLow?.let {
                    Text(
                        it,
                        style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp),
                        maxLines = 1,
                    )
                }
            }
            if (wide && data.hours.isNotEmpty()) {
                Spacer(GlanceModifier.width(12.dp))
                data.hours.forEach { hour ->
                    Column(
                        GlanceModifier.padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            hour.label,
                            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                        )
                        Text(
                            hour.temperature,
                            style = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                        )
                    }
                }
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

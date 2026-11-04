// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import org.prauga.weather.R

abstract class WeatherGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single
    override val previewSizeMode: PreviewSizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.load(context)
        val extra = prepare(context, data, id)
        provideContent { GlanceTheme { WidgetFrame { Body(data, extra) } } }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = WidgetData.sample(context)
        val extra = prepare(context, data, null)
        provideContent { GlanceTheme { WidgetFrame { Body(data, extra) } } }
    }

    protected open suspend fun prepare(context: Context, data: WidgetData, id: GlanceId?): Bitmap? =
        null

    @Composable protected abstract fun ColumnScope.Body(data: WidgetData, bitmap: Bitmap?)

    companion object {
        val all: List<WeatherGlanceWidget>
            get() =
                listOf(
                    ForecastWidget(),
                    HourlyWidget(),
                    DailyWidget(),
                    DetailsWidget(),
                    SunWidget(),
                )

        suspend fun updateAll(context: Context) = all.forEach { it.updateAll(context) }
    }
}

class ForecastWidget : WeatherGlanceWidget() {
    @Composable
    override fun ColumnScope.Body(data: WidgetData, bitmap: Bitmap?) {
        NowHeader(data, wide = false)
    }
}

class HourlyWidget : WeatherGlanceWidget() {
    @Composable
    override fun ColumnScope.Body(data: WidgetData, bitmap: Bitmap?) {
        NowHeader(data, wide = true)
        Spacer(GlanceModifier.defaultWeight())
        HourRow(data.hours)
    }
}

class DailyWidget : WeatherGlanceWidget() {
    override val sizeMode = SizeMode.Responsive(HEIGHTS)
    override val previewSizeMode = SizeMode.Responsive(HEIGHTS)

    @Composable
    override fun ColumnScope.Body(data: WidgetData, bitmap: Bitmap?) {
        val rows =
            ((LocalSize.current.height.value - FIXED_HEIGHT_DP) / ROW_HEIGHT_DP)
                .toInt()
                .coerceIn(0, 5)
        NowHeader(data, wide = true)
        Spacer(GlanceModifier.height(6.dp))
        HourRow(data.hours)
        if (rows > 0) {
            Spacer(GlanceModifier.height(6.dp))
            DayRows(data.days.drop(1).take(rows))
        }
    }

    private companion object {
        const val FIXED_HEIGHT_DP = 188f
        const val ROW_HEIGHT_DP = 24f
        val HEIGHTS =
            listOf(200, 224, 248, 272, 296, 320, 344).map { DpSize(250.dp, it.dp) }.toSet()
    }
}

class DetailsWidget : WeatherGlanceWidget() {
    @Composable
    override fun ColumnScope.Body(data: WidgetData, bitmap: Bitmap?) {
        Row(GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.defaultWeight()) { NowHeader(data, wide = false) }
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight()) { DetailGrid(data.details) }
        }
    }
}

class SunWidget : WeatherGlanceWidget() {
    override suspend fun prepare(context: Context, data: WidgetData, id: GlanceId?): Bitmap? {
        val sunrise = data.sunrise ?: return null
        val sunset = data.sunset ?: return null
        val width =
            id?.let {
                AppWidgetManager.getInstance(context)
                    .getAppWidgetOptions(GlanceAppWidgetManager(context).getAppWidgetId(it))
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    .takeIf { width -> width > 0 }
            } ?: PREVIEW_WIDTH_DP
        return SunWave.render(
            context,
            (width - FRAME_PADDING_DP).coerceAtLeast(MIN_WAVE_DP),
            ((width - FRAME_PADDING_DP) / WAVE_ASPECT).coerceAtLeast(MIN_WAVE_DP),
            sunrise,
            sunset,
            data.now,
            data.zone,
        )
    }

    @Composable
    override fun ColumnScope.Body(data: WidgetData, bitmap: Bitmap?) {
        val context = androidx.glance.LocalContext.current
        val now = data.now
        val nextIsSunset =
            data.sunrise != null &&
                data.sunset != null &&
                now.isAfter(data.sunrise) &&
                now.isBefore(data.sunset)
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (nextIsSunset) R.drawable.ic_w_sunset else R.drawable.ic_w_sunrise, 20.dp)
            Spacer(GlanceModifier.width(6.dp))
            Column(GlanceModifier.defaultWeight()) {
                Label(
                    context.getString(
                        if (nextIsSunset) R.string.widget_sunset else R.string.widget_sunrise
                    ),
                    muted = true,
                )
                Label(
                    (if (nextIsSunset) data.sunsetText else data.sunriseText) ?: "–",
                    size = 24.sp,
                )
            }
            Label(data.place, muted = true)
        }
        Spacer(GlanceModifier.height(6.dp))
        if (bitmap != null) {
            Image(
                ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Spacer(GlanceModifier.defaultWeight())
        }
        Row(GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.defaultWeight()) {
                Label(
                    context.getString(R.string.widget_sunrise_at, data.sunriseText ?: "–"),
                    size = 12.sp,
                    muted = true,
                )
            }
            Label(
                context.getString(R.string.widget_sunset_at, data.sunsetText ?: "–"),
                size = 12.sp,
                muted = true,
            )
        }
    }

    private companion object {
        const val PREVIEW_WIDTH_DP = 360
        const val FRAME_PADDING_DP = 32
        const val WAVE_ASPECT = 8
        const val MIN_WAVE_DP = 24
    }
}

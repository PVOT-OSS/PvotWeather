// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager

abstract class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetScheduler.sync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetScheduler.sync(context)
    }
}

class ForecastWidgetReceiver : WeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ForecastWidget()
}

class HourlyWidgetReceiver : WeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HourlyWidget()
}

class DailyWidgetReceiver : WeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyWidget()
}

class DetailsWidgetReceiver : WeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DetailsWidget()
}

class SunWidgetReceiver : WeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SunWidget()
}

object WidgetScheduler {
    private val receivers =
        listOf(
            ForecastWidgetReceiver::class.java,
            HourlyWidgetReceiver::class.java,
            DailyWidgetReceiver::class.java,
            DetailsWidgetReceiver::class.java,
            SunWidgetReceiver::class.java,
        )

    fun sync(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val count = receivers.sumOf { manager.getAppWidgetIds(ComponentName(context, it)).size }
        val work = WorkManager.getInstance(context)
        if (count > 0) {
            work.enqueueUniquePeriodicWork(
                WidgetRefreshWorker.NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                WidgetRefreshWorker.request(),
            )
        } else {
            work.cancelUniqueWork(WidgetRefreshWorker.NAME)
        }
    }
}

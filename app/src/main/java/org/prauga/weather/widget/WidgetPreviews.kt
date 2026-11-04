// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager

object WidgetPreviews {
    private val receivers =
        listOf(
            ForecastWidgetReceiver::class,
            HourlyWidgetReceiver::class,
            DailyWidgetReceiver::class,
            DetailsWidgetReceiver::class,
            SunWidgetReceiver::class,
        )

    suspend fun publish(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        if (prefs.getLong(KEY_INSTALLED, 0) == installed) return
        val manager = GlanceAppWidgetManager(context)
        val published =
            receivers
                .map { receiver ->
                    runCatching { manager.setWidgetPreviews(receiver) }.getOrNull() ==
                        GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS
                }
                .all { it }
        if (published) prefs.edit { putLong(KEY_INSTALLED, installed) }
    }

    private const val PREFS = "widget_previews"
    private const val KEY_INSTALLED = "published_for_install"
}

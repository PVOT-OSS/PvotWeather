// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import org.prauga.weather.WeatherApplication
import org.prauga.weather.core.model.Provider

class WidgetRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as WeatherApplication).container
        val places = container.places.observeAll().first()
        val selectedId = container.preferences.selectedPlaceId.first()
        val place = places.find { it.id == selectedId } ?: places.firstOrNull()
        // MET's terms: no fetching while the app is not in use, so the widget refreshes Open-Meteo
        // only.
        if (place != null)
            container.forecasts.refresh(place, providers = setOf(Provider.OPEN_METEO))
        WeatherWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val NAME = "widget-refresh"

        fun request() =
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
    }
}

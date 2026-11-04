// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.content.Context
import android.text.format.DateFormat
import kotlinx.coroutines.flow.first
import org.prauga.weather.R
import org.prauga.weather.WeatherApplication
import org.prauga.weather.forecast.Formats

data class WidgetHour(val label: String, val temperature: String)

data class WidgetData(
    val place: String,
    val temperature: String?,
    val condition: String?,
    val highLow: String?,
    val hours: List<WidgetHour>,
) {
    companion object {
        suspend fun load(context: Context): WidgetData {
            val container = (context.applicationContext as WeatherApplication).container
            val places = container.places.observeAll().first()
            val selectedId = container.preferences.selectedPlaceId.first()
            val place =
                places.find { it.id == selectedId }
                    ?: places.firstOrNull()
                    ?: return WidgetData(
                        context.getString(R.string.no_places),
                        null,
                        null,
                        null,
                        emptyList(),
                    )
            val unit = container.preferences.temperatureUnit.first()
            val forecast = container.forecasts.observe(place).first().forecast
            val title =
                when {
                    place.name.isBlank() -> context.getString(R.string.current_location)
                    else -> place.name
                }
            fun degrees(celsius: Double?) = celsius?.let { "${Formats.degrees(it, unit)}°" }
            val current = forecast?.current
            val today = forecast?.days?.firstOrNull()
            val is24 = DateFormat.is24HourFormat(context)
            return WidgetData(
                place = title,
                temperature = degrees(current?.temperature?.value),
                condition =
                    current?.condition?.value?.let {
                        context.getString(Formats.condition(it, current.isDay))
                    },
                highLow =
                    today?.let { d ->
                        listOfNotNull(
                                degrees(d.temperatureMax?.value)?.let { "↑$it" },
                                degrees(d.temperatureMin?.value)?.let { "↓$it" },
                            )
                            .joinToString("  ")
                            .ifEmpty { null }
                    },
                hours =
                    forecast?.hours.orEmpty().drop(1).take(4).map { h ->
                        WidgetHour(
                            Formats.hour(h.time, place.zone, is24),
                            degrees(h.temperature?.value) ?: "–",
                        )
                    },
            )
        }
    }
}

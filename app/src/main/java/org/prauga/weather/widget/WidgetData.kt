// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import org.prauga.weather.R
import org.prauga.weather.WeatherApplication
import org.prauga.weather.core.model.Condition
import org.prauga.weather.forecast.Formats
import org.prauga.weather.settings.symbol

data class WidgetHour(val label: String, @DrawableRes val icon: Int, val temperature: String)

data class WidgetDay(
    val label: String,
    @DrawableRes val icon: Int,
    val high: String,
    val low: String,
    val rain: String?,
)

data class WidgetDetail(@DrawableRes val icon: Int, val label: String, val value: String)

data class WidgetData(
    val place: String,
    val temperature: String? = null,
    val condition: String? = null,
    @DrawableRes val icon: Int? = null,
    val highLow: String? = null,
    val hours: List<WidgetHour> = emptyList(),
    val days: List<WidgetDay> = emptyList(),
    val details: List<WidgetDetail> = emptyList(),
    val sunrise: Instant? = null,
    val sunset: Instant? = null,
    val sunriseText: String? = null,
    val sunsetText: String? = null,
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    companion object {
        suspend fun load(context: Context): WidgetData {
            val container = (context.applicationContext as WeatherApplication).container
            val places = container.places.observeAll().first()
            val selectedId = container.preferences.selectedPlaceId.first()
            val place =
                places.find { it.id == selectedId }
                    ?: places.firstOrNull()
                    ?: return WidgetData(context.getString(R.string.no_places))
            val unit = container.preferences.temperatureUnit.first()
            val windUnit = container.preferences.windUnit.first()
            val forecast = container.forecasts.observe(place).first().forecast
            val zone = place.zone
            val is24 = DateFormat.is24HourFormat(context)
            fun degrees(celsius: Double?) = celsius?.let { "${Formats.degrees(it, unit)}°" }
            fun percent(value: Double?) =
                value?.let { context.getString(R.string.percent, it.toInt()) }

            val current = forecast?.current
            val today = forecast?.days?.firstOrNull()
            val details = buildList {
                percent(today?.precipitationProbability?.value)?.let {
                    add(
                        WidgetDetail(
                            R.drawable.ic_w_umbrella,
                            context.getString(R.string.widget_rain_chance),
                            it,
                        )
                    )
                }
                current?.uvIndex?.value?.let {
                    add(
                        WidgetDetail(
                            R.drawable.ic_w_uv,
                            context.getString(R.string.uv_index),
                            "${it.toInt()} " + context.getString(Formats.uvLevel(it)),
                        )
                    )
                }
                current?.windSpeed?.value?.let {
                    add(
                        WidgetDetail(
                            R.drawable.ic_w_wind,
                            context.getString(R.string.wind),
                            context.getString(
                                R.string.wind_speed,
                                Formats.wind(it, windUnit),
                                context.getString(windUnit.symbol),
                            ),
                        )
                    )
                }
                percent(current?.humidity?.value)?.let {
                    add(
                        WidgetDetail(
                            R.drawable.ic_w_droplet,
                            context.getString(R.string.humidity),
                            it,
                        )
                    )
                }
                degrees(current?.apparentTemperature?.value)?.let {
                    add(
                        WidgetDetail(
                            R.drawable.ic_w_temperature,
                            context.getString(R.string.feels_like),
                            it,
                        )
                    )
                }
                current?.visibility?.value?.let {
                    add(
                        WidgetDetail(
                            R.drawable.ic_w_eye,
                            context.getString(R.string.visibility),
                            context.getString(R.string.visibility_km, Formats.km(it)),
                        )
                    )
                }
            }
            return WidgetData(
                place = place.name.ifBlank { context.getString(R.string.current_location) },
                temperature = degrees(current?.temperature?.value),
                condition =
                    current?.condition?.value?.let {
                        context.getString(Formats.condition(it, current.isDay))
                    },
                icon = current?.condition?.value?.let { conditionIcon(it, current.isDay) },
                highLow =
                    today?.let { d ->
                        val high = degrees(d.temperatureMax?.value)
                        val low = degrees(d.temperatureMin?.value)
                        if (high != null && low != null)
                            context.getString(R.string.widget_high_low, high, low)
                        else null
                    },
                hours =
                    forecast?.hours.orEmpty().take(6).mapIndexed { i, h ->
                        val shown = if (i == 0) current ?: h else h
                        WidgetHour(
                            if (i == 0) context.getString(R.string.now)
                            else Formats.hour(h.time, zone, is24),
                            conditionIcon(shown.condition?.value, shown.isDay),
                            degrees(shown.temperature?.value) ?: "–",
                        )
                    },
                days =
                    forecast?.days.orEmpty().take(6).mapIndexed { i, d ->
                        WidgetDay(
                            when (i) {
                                0 -> context.getString(R.string.today)
                                else -> Formats.weekday(d.date)
                            },
                            conditionIcon(d.condition?.value, true),
                            degrees(d.temperatureMax?.value) ?: "–",
                            degrees(d.temperatureMin?.value) ?: "–",
                            d.precipitationProbability
                                ?.value
                                ?.takeIf { it >= 10 }
                                ?.let { percent(it) },
                        )
                    },
                details = details,
                sunrise = today?.sunrise,
                sunset = today?.sunset,
                sunriseText = today?.sunrise?.let { Formats.time(it, zone, is24) },
                sunsetText = today?.sunset?.let { Formats.time(it, zone, is24) },
                zone = zone,
            )
        }

        @DrawableRes
        fun conditionIcon(condition: Condition?, isDay: Boolean?): Int =
            when (condition) {
                null,
                Condition.CLEAR -> if (isDay == false) R.drawable.ic_w_moon else R.drawable.ic_w_sun
                Condition.PARTLY_CLOUDY ->
                    if (isDay == false) R.drawable.ic_w_cloud else R.drawable.ic_w_partly
                Condition.CLOUDY -> R.drawable.ic_w_cloud
                Condition.FOG -> R.drawable.ic_w_fog
                Condition.DRIZZLE,
                Condition.RAIN_SHOWERS,
                Condition.RAIN,
                Condition.HEAVY_RAIN -> R.drawable.ic_w_rain
                Condition.SLEET,
                Condition.SNOW_SHOWERS,
                Condition.SNOW,
                Condition.HEAVY_SNOW -> R.drawable.ic_w_snow
                Condition.THUNDERSTORM -> R.drawable.ic_w_storm
            }
    }
}

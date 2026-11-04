// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import java.time.Instant
import java.time.LocalDate
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
    val now: Instant = Instant.now(),
) {
    companion object {
        fun sample(context: Context): WidgetData {
            val zone = ZoneId.of("Asia/Kolkata")
            val today = LocalDate.now(zone)
            val now = today.atTime(14, 0).atZone(zone).toInstant()
            val is24 = DateFormat.is24HourFormat(context)
            fun degrees(value: Int) = "$value°"
            val hourTemps = listOf(26, 27, 27, 26, 25, 24)
            val hourIcons =
                listOf(
                    R.drawable.ic_w_partly,
                    R.drawable.ic_w_partly,
                    R.drawable.ic_w_cloud,
                    R.drawable.ic_w_rain,
                    R.drawable.ic_w_rain,
                    R.drawable.ic_w_cloud,
                )
            val sunrise = today.atTime(6, 13).atZone(zone).toInstant()
            val sunset = today.atTime(17, 51).atZone(zone).toInstant()
            return WidgetData(
                place = SAMPLE_PLACE,
                temperature = degrees(26),
                condition = context.getString(R.string.condition_partly_cloudy),
                icon = R.drawable.ic_w_partly,
                highLow = context.getString(R.string.widget_high_low, degrees(29), degrees(20)),
                hours =
                    hourTemps.indices.map { i ->
                        WidgetHour(
                            if (i == 0) context.getString(R.string.now)
                            else Formats.hour(now.plusSeconds(3600L * i), zone, is24),
                            hourIcons[i],
                            degrees(hourTemps[i]),
                        )
                    },
                days =
                    listOf(
                            Triple(R.drawable.ic_w_partly, 29 to 20, null),
                            Triple(R.drawable.ic_w_rain, 28 to 20, 60),
                            Triple(R.drawable.ic_w_rain, 27 to 19, 80),
                            Triple(R.drawable.ic_w_cloud, 28 to 19, 30),
                            Triple(R.drawable.ic_w_partly, 29 to 20, null),
                            Triple(R.drawable.ic_w_sun, 30 to 20, null),
                        )
                        .mapIndexed { i, (icon, temps, rain) ->
                            WidgetDay(
                                if (i == 0) context.getString(R.string.today)
                                else Formats.weekday(today.plusDays(i.toLong())),
                                icon,
                                degrees(temps.first),
                                degrees(temps.second),
                                rain?.let { context.getString(R.string.percent, it) },
                            )
                        },
                details =
                    listOf(
                        WidgetDetail(
                            R.drawable.ic_w_umbrella,
                            context.getString(R.string.widget_rain_chance),
                            context.getString(R.string.percent, 20),
                        ),
                        WidgetDetail(
                            R.drawable.ic_w_uv,
                            context.getString(R.string.uv_index),
                            "6 " + context.getString(R.string.uv_high),
                        ),
                        WidgetDetail(
                            R.drawable.ic_w_wind,
                            context.getString(R.string.wind),
                            context.getString(
                                R.string.wind_speed,
                                12,
                                context.getString(R.string.unit_kmh),
                            ),
                        ),
                        WidgetDetail(
                            R.drawable.ic_w_droplet,
                            context.getString(R.string.humidity),
                            context.getString(R.string.percent, 58),
                        ),
                        WidgetDetail(
                            R.drawable.ic_w_temperature,
                            context.getString(R.string.feels_like),
                            degrees(27),
                        ),
                        WidgetDetail(
                            R.drawable.ic_w_eye,
                            context.getString(R.string.visibility),
                            context.getString(R.string.visibility_km, "10"),
                        ),
                    ),
                sunrise = sunrise,
                sunset = sunset,
                sunriseText = Formats.time(sunrise, zone, is24),
                sunsetText = Formats.time(sunset, zone, is24),
                zone = zone,
                now = now,
            )
        }

        private const val SAMPLE_PLACE = "Bengaluru"

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

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.annotation.StringRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import org.prauga.weather.R
import org.prauga.weather.core.model.Condition

object Formats {
    fun degrees(value: Double): Int = value.roundToInt()

    fun kmh(metresPerSecond: Double): Int = (metresPerSecond * 3.6).roundToInt()

    fun km(metres: Double): String =
        if (metres >= 10_000) (metres / 1000).roundToInt().toString()
        else String.format(Locale.getDefault(), "%.1f", metres / 1000)

    fun hour(time: Instant, zone: ZoneId, is24Hour: Boolean): String =
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h a", Locale.getDefault())
            .format(time.atZone(zone))

    fun time(time: Instant, zone: ZoneId, is24Hour: Boolean): String =
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
            .format(time.atZone(zone))

    fun weekday(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    @StringRes
    fun condition(condition: Condition, isDay: Boolean?): Int =
        when (condition) {
            Condition.CLEAR ->
                if (isDay == true) R.string.condition_clear_day else R.string.condition_clear
            Condition.PARTLY_CLOUDY -> R.string.condition_partly_cloudy
            Condition.CLOUDY -> R.string.condition_cloudy
            Condition.FOG -> R.string.condition_fog
            Condition.DRIZZLE -> R.string.condition_drizzle
            Condition.RAIN_SHOWERS -> R.string.condition_rain_showers
            Condition.RAIN -> R.string.condition_rain
            Condition.SLEET -> R.string.condition_sleet
            Condition.SNOW_SHOWERS -> R.string.condition_snow_showers
            Condition.SNOW -> R.string.condition_snow
            Condition.HEAVY_RAIN -> R.string.condition_heavy_rain
            Condition.HEAVY_SNOW -> R.string.condition_heavy_snow
            Condition.THUNDERSTORM -> R.string.condition_thunderstorm
        }

    @StringRes
    fun uvLevel(index: Double): Int =
        when {
            index < 3 -> R.string.uv_low
            index < 6 -> R.string.uv_moderate
            index < 8 -> R.string.uv_high
            index < 11 -> R.string.uv_very_high
            else -> R.string.uv_extreme
        }

    @StringRes
    fun visibility(metres: Double): Int =
        when {
            metres >= 10_000 -> R.string.visibility_clear
            metres >= 4_000 -> R.string.visibility_haze
            metres >= 1_000 -> R.string.visibility_hazy
            else -> R.string.visibility_fog
        }

    @StringRes
    fun feelsLike(actual: Double, apparent: Double): Int =
        when {
            apparent - actual >= 2 -> R.string.feels_warmer
            actual - apparent >= 2 -> R.string.feels_colder
            else -> R.string.feels_same
        }

    @StringRes
    fun compass(degrees: Double): Int =
        listOf(
            R.string.compass_n,
            R.string.compass_ne,
            R.string.compass_e,
            R.string.compass_se,
            R.string.compass_s,
            R.string.compass_sw,
            R.string.compass_w,
            R.string.compass_nw,
        )[((degrees % 360 + 360 + 22.5) / 45).toInt() % 8]
}

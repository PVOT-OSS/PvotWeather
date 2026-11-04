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
import org.prauga.weather.core.model.Pollen
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.core.model.WindUnit

object Formats {
    fun degrees(celsius: Double, unit: TemperatureUnit = TemperatureUnit.CELSIUS): Int =
        unit.fromCelsius(celsius).roundToInt()

    fun wind(metresPerSecond: Double, unit: WindUnit = WindUnit.KILOMETRES_PER_HOUR): Int =
        unit.fromMetresPerSecond(metresPerSecond).roundToInt()

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

    fun millimetres(value: Double): String =
        if (value >= 10) value.roundToInt().toString()
        else String.format(Locale.getDefault(), "%.1f", value)

    fun precipitation(mm: Double, imperial: Boolean): String =
        if (imperial) decimals(mm / MM_PER_INCH, 2) else millimetres(mm)

    fun snowDepth(metres: Double, imperial: Boolean): String =
        (if (imperial) metres * INCHES_PER_METRE else metres * 100).roundToInt().toString()

    fun snowfall(cm: Double, imperial: Boolean): String =
        decimals(if (imperial) cm / CM_PER_INCH else cm, 1)

    fun pressure(hPa: Double, imperial: Boolean): String =
        if (imperial) decimals(hPa * INHG_PER_HPA, 2) else hPa.roundToInt().toString()

    fun waves(metres: Double, imperial: Boolean): String =
        decimals(if (imperial) metres * FEET_PER_METRE else metres, 1)

    fun visibility(metres: Double, imperial: Boolean): String {
        if (!imperial) return km(metres)
        val miles = metres / METRES_PER_MILE
        return if (miles >= 10) miles.roundToInt().toString() else decimals(miles, 1)
    }

    fun flow(m3s: Double, imperial: Boolean): String =
        flow(if (imperial) m3s * CUBIC_FEET_PER_M3 else m3s)

    private fun decimals(value: Double, places: Int): String =
        String.format(Locale.getDefault(), "%.${places}f", value)

    private const val MM_PER_INCH = 25.4
    private const val CM_PER_INCH = 2.54
    private const val INCHES_PER_METRE = 39.3701
    private const val INHG_PER_HPA = 0.0295300
    private const val FEET_PER_METRE = 3.28084
    private const val METRES_PER_MILE = 1609.344
    private const val CUBIC_FEET_PER_M3 = 35.3147

    fun hours(seconds: Double): String = String.format(Locale.getDefault(), "%.1f", seconds / 3600)

    fun flow(value: Double): String =
        if (value >= 10) value.roundToInt().toString()
        else String.format(Locale.getDefault(), "%.2f", value)

    @StringRes
    fun pressureTrend(now: Double, later: Double?): Int =
        when {
            later == null -> R.string.pressure_sea_level
            later - now >= 1 -> R.string.pressure_rising
            now - later >= 1 -> R.string.pressure_falling
            else -> R.string.pressure_steady
        }

    @StringRes
    fun moonPhase(degrees: Double): Int {
        val d = (degrees % 360 + 360) % 360
        return when {
            d < 11.25 || d >= 348.75 -> R.string.moon_new
            d < 78.75 -> R.string.moon_waxing_crescent
            d < 101.25 -> R.string.moon_first_quarter
            d < 168.75 -> R.string.moon_waxing_gibbous
            d < 191.25 -> R.string.moon_full
            d < 258.75 -> R.string.moon_waning_gibbous
            d < 281.25 -> R.string.moon_last_quarter
            else -> R.string.moon_waning_crescent
        }
    }

    fun moonIllumination(degrees: Double): Int =
        ((1 - kotlin.math.cos(Math.toRadians(degrees))) / 2 * 100).roundToInt()

    @StringRes
    fun riverLevel(discharge: Double, mean: Double): Int =
        when {
            mean <= 0 -> R.string.river_normal
            discharge >= mean * 2 -> R.string.river_very_high
            discharge >= mean * 1.3 -> R.string.river_high
            discharge <= mean * 0.7 -> R.string.river_low
            else -> R.string.river_normal
        }

    @StringRes
    fun pollenName(pollen: Pollen): Int =
        when (pollen) {
            Pollen.ALDER -> R.string.pollen_alder
            Pollen.BIRCH -> R.string.pollen_birch
            Pollen.GRASS -> R.string.pollen_grass
            Pollen.MUGWORT -> R.string.pollen_mugwort
            Pollen.OLIVE -> R.string.pollen_olive
            Pollen.RAGWEED -> R.string.pollen_ragweed
        }

    @StringRes
    fun europeanAqiLevel(aqi: Int): Int =
        when {
            aqi <= 20 -> R.string.eaqi_good
            aqi <= 40 -> R.string.eaqi_fair
            aqi <= 60 -> R.string.eaqi_moderate
            aqi <= 80 -> R.string.eaqi_poor
            aqi <= 100 -> R.string.eaqi_very_poor
            else -> R.string.eaqi_extremely_poor
        }

    @StringRes
    fun metAqiLevel(index: Double): Int =
        when {
            index < 2 -> R.string.met_aqi_low
            index < 3 -> R.string.met_aqi_moderate
            index < 4 -> R.string.met_aqi_high
            else -> R.string.met_aqi_very_high
        }

    @StringRes
    fun aqiLevel(aqi: Int): Int =
        when {
            aqi <= 50 -> R.string.aqi_good
            aqi <= 100 -> R.string.aqi_moderate
            aqi <= 150 -> R.string.aqi_sensitive
            aqi <= 200 -> R.string.aqi_unhealthy
            aqi <= 300 -> R.string.aqi_very_unhealthy
            else -> R.string.aqi_hazardous
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

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.prauga.weather.core.model.Condition
import org.prauga.weather.core.model.DayPoint
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.HourPoint
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.model.ProviderDay
import org.prauga.weather.core.model.ProviderForecast
import org.prauga.weather.core.model.ProviderHour
import org.prauga.weather.core.model.Sourced
import org.prauga.weather.core.model.SourcedCondition

class ForecastMerger {

    fun merge(
        place: Place,
        openMeteo: ProviderForecast?,
        metNorway: ProviderForecast?,
        now: Instant,
        openMeteoModel: String? = null,
    ): Forecast? {
        if (openMeteo == null && metNorway == null) return null
        val met = MetTimeline(metNorway?.hours.orEmpty())
        val grid = openMeteo?.hours?.takeIf { it.isNotEmpty() }
        val hours =
            if (grid != null) {
                grid
                    .filter { (it.time + HOUR).isAfter(now) }
                    .map { mergeHour(it.time, it, met.at(it.time)) }
            } else {
                met.hours
                    .filter { (it.time + HOUR).isAfter(now) }
                    .map { mergeHour(it.time, null, it) }
            }
        val metCurrent =
            met.at(now)
                ?: metNorway?.current?.takeIf {
                    Duration.between(it.time, now).abs() <= MAX_CURRENT_AGE
                }
        val omCurrent =
            openMeteo?.current?.takeIf { Duration.between(it.time, now).abs() <= MAX_CURRENT_AGE }
        val current =
            if (omCurrent == null && metCurrent == null) hours.firstOrNull()
            else mergeHour(now, omCurrent, metCurrent)
        val omDays = openMeteo?.days.orEmpty().associateBy { it.date }
        val metDays = metNorway?.days.orEmpty().associateBy { it.date }
        val today = now.atZone(place.zone).toLocalDate()
        val days =
            (omDays.keys + metDays.keys)
                .filter { !it.isBefore(today) }
                .sorted()
                .map { mergeDay(omDays[it], metDays[it]) }
        val providers = buildSet {
            if (openMeteo != null) add(Provider.OPEN_METEO)
            if (metNorway != null) add(Provider.MET_NORWAY)
        }
        val yesterday =
            today.minusDays(1).let { d ->
                if (omDays[d] != null || metDays[d] != null) mergeDay(omDays[d], metDays[d])
                else null
            }
        return Forecast(
            place,
            current,
            hours,
            days,
            providers,
            openMeteo?.minutely.orEmpty(),
            yesterday,
            openMeteoModel,
        )
    }

    internal fun mergeHour(time: Instant, om: ProviderHour?, met: ProviderHour?) =
        HourPoint(
            time = time,
            temperature = mean(om?.temperature, met?.temperature),
            apparentTemperature = mean(om?.apparentTemperature, met?.apparentTemperature),
            humidity = mean(om?.humidity, met?.humidity),
            dewPoint = mean(om?.dewPoint, met?.dewPoint),
            pressure = mean(om?.pressure, met?.pressure),
            cloudCover = mean(om?.cloudCover, met?.cloudCover),
            windSpeed = mean(om?.windSpeed, met?.windSpeed),
            windDirection = circularMean(om?.windDirection, met?.windDirection),
            windGust = mean(om?.windGust, met?.windGust),
            // MET's UV is clear-sky only.
            uvIndex = prefer(om?.uvIndex, met?.uvIndex),
            visibility = mean(om?.visibility, met?.visibility),
            precipitation = mean(om?.precipitation, met?.precipitation),
            precipitationProbability =
                mean(om?.precipitationProbability, met?.precipitationProbability),
            condition = condition(om?.condition, met?.condition),
            isDay = om?.isDay ?: met?.isDay,
            temperatureLow = met?.temperatureLow,
            temperatureHigh = met?.temperatureHigh,
            windLow = met?.windLow,
            windHigh = met?.windHigh,
            precipitationMin = met?.precipitationMin,
            precipitationMax = met?.precipitationMax,
            thunderProbability = mean(null, met?.thunderProbability),
            fog = mean(null, met?.fog),
            cloudLow = mean(om?.cloudLow, met?.cloudLow),
            cloudMid = mean(om?.cloudMid, met?.cloudMid),
            cloudHigh = mean(om?.cloudHigh, met?.cloudHigh),
            snowfall = mean(om?.snowfall, null),
            snowDepth = mean(om?.snowDepth, null),
        )

    internal fun mergeDay(om: ProviderDay?, met: ProviderDay?): DayPoint {
        val date = requireNotNull(om?.date ?: met?.date)
        return DayPoint(
            date = date,
            temperatureMax = mean(om?.temperatureMax, met?.temperatureMax),
            temperatureMin = mean(om?.temperatureMin, met?.temperatureMin),
            precipitationSum = mean(om?.precipitationSum, met?.precipitationSum),
            precipitationProbability =
                mean(om?.precipitationProbability, met?.precipitationProbability),
            condition = condition(om?.condition, met?.condition),
            uvIndexMax = prefer(om?.uvIndexMax, met?.uvIndexMax),
            sunrise = om?.sunrise ?: met?.sunrise,
            sunset = om?.sunset ?: met?.sunset,
            precipitationMin = met?.precipitationMin,
            precipitationMax = met?.precipitationMax,
            snowfallSum = mean(om?.snowfallSum, null),
            sunshineSeconds = om?.sunshineSeconds,
            daylightSeconds = om?.daylightSeconds,
        )
    }

    internal class MetTimeline(val hours: List<ProviderHour>) {
        private val byTime = hours.associateBy { it.time }

        fun at(time: Instant): ProviderHour? {
            byTime[time]?.let {
                return it
            }
            val before = byTime[time.truncatedTo(ChronoUnit.HOURS)] ?: return null
            val after = byTime[before.time + HOUR] ?: return null
            val f = Duration.between(before.time, time).toMillis().toDouble() / HOUR.toMillis()
            fun lerp(a: Double?, b: Double?) =
                if (a != null && b != null) a + (b - a) * f else a ?: b
            return before.copy(
                time = time,
                temperature = lerp(before.temperature, after.temperature),
                apparentTemperature = lerp(before.apparentTemperature, after.apparentTemperature),
                humidity = lerp(before.humidity, after.humidity),
                dewPoint = lerp(before.dewPoint, after.dewPoint),
                pressure = lerp(before.pressure, after.pressure),
                cloudCover = lerp(before.cloudCover, after.cloudCover),
                windSpeed = lerp(before.windSpeed, after.windSpeed),
                windGust = lerp(before.windGust, after.windGust),
                windDirection = if (f < 0.5) before.windDirection else after.windDirection,
                uvIndex = lerp(before.uvIndex, after.uvIndex),
                temperatureLow = lerp(before.temperatureLow, after.temperatureLow),
                temperatureHigh = lerp(before.temperatureHigh, after.temperatureHigh),
                windLow = lerp(before.windLow, after.windLow),
                windHigh = lerp(before.windHigh, after.windHigh),
                fog = lerp(before.fog, after.fog),
                cloudLow = lerp(before.cloudLow, after.cloudLow),
                cloudMid = lerp(before.cloudMid, after.cloudMid),
                cloudHigh = lerp(before.cloudHigh, after.cloudHigh),
            )
        }
    }

    companion object {
        private val HOUR: Duration = Duration.ofHours(1)

        private val MAX_CURRENT_AGE: Duration = Duration.ofMinutes(90)

        internal fun mean(om: Double?, met: Double?): Sourced? =
            when {
                om != null && met != null -> Sourced((om + met) / 2, om, met)
                om != null -> Sourced(om, om, null)
                met != null -> Sourced(met, null, met)
                else -> null
            }

        internal fun prefer(om: Double?, met: Double?): Sourced? =
            (om ?: met)?.let { Sourced(it, om, met) }

        internal fun circularMean(om: Double?, met: Double?): Sourced? {
            if (om == null || met == null) return mean(om, met)
            val x = cos(Math.toRadians(om)) + cos(Math.toRadians(met))
            val y = sin(Math.toRadians(om)) + sin(Math.toRadians(met))
            val degrees = (Math.toDegrees(atan2(y, x)) + 360) % 360
            return Sourced(degrees, om, met)
        }

        internal fun condition(om: Condition?, met: Condition?): SourcedCondition? =
            Condition.mostSevere(om, met)?.let { SourcedCondition(it, om, met) }
    }
}

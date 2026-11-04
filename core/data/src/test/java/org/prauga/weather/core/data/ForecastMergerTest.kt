// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.model.Condition
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.model.ProviderDay
import org.prauga.weather.core.model.ProviderForecast
import org.prauga.weather.core.model.ProviderHour

class ForecastMergerTest {
    private val merger = ForecastMerger()
    private val place =
        Place.of("Bengaluru", null, 12.9716, 77.5946, 920, ZoneId.of("Asia/Kolkata"))
    private val now = Instant.parse("2026-11-04T07:20:00Z")
    private val h7 = Instant.parse("2026-11-04T07:00:00Z")
    private val h8 = Instant.parse("2026-11-04T08:00:00Z")

    private fun om(
        vararg hours: ProviderHour,
        days: List<ProviderDay> = emptyList(),
        current: ProviderHour? = null,
    ) = ProviderForecast(Provider.OPEN_METEO, current, hours.toList(), days)

    private fun met(vararg hours: ProviderHour, days: List<ProviderDay> = emptyList()) =
        ProviderForecast(Provider.MET_NORWAY, hours.firstOrNull(), hours.toList(), days)

    @Test
    fun bothPresentBlendsAndKeepsOriginals() {
        val f =
            merger.merge(
                place,
                om(ProviderHour(h8, temperature = 28.0, condition = Condition.CLOUDY)),
                met(ProviderHour(h8, temperature = 26.0, condition = Condition.RAIN)),
                now,
            )!!
        val hour = f.hours.single()
        assertEquals(27.0, hour.temperature!!.value, 1e-9)
        assertEquals(28.0, hour.temperature!!.openMeteo!!, 1e-9)
        assertEquals(26.0, hour.temperature!!.metNorway!!, 1e-9)
        assertEquals(2.0, hour.temperature!!.spread!!, 1e-9)
        assertEquals(Condition.RAIN, hour.condition!!.value)
        assertTrue(hour.condition!!.differs)
        assertEquals(setOf(Provider.OPEN_METEO, Provider.MET_NORWAY), f.providers)
    }

    @Test
    fun oneProviderFillsGaps() {
        val f =
            merger.merge(
                place,
                om(
                    ProviderHour(
                        h8,
                        temperature = 28.0,
                        precipitationProbability = 40.0,
                        visibility = 24000.0,
                    )
                ),
                met(ProviderHour(h8, temperature = 26.0)),
                now,
            )!!
        val hour = f.hours.single()
        assertEquals(40.0, hour.precipitationProbability!!.value, 1e-9)
        assertNull(hour.precipitationProbability!!.spread)
        assertEquals(setOf(Provider.OPEN_METEO), hour.visibility!!.providers)
    }

    @Test
    fun onlyOneProviderStillGivesAForecast() {
        val f = merger.merge(place, null, met(ProviderHour(h8, temperature = 26.0)), now)!!
        assertEquals(setOf(Provider.MET_NORWAY), f.providers)
        assertEquals(26.0, f.hours.single().temperature!!.value, 1e-9)
        assertNull(merger.merge(place, null, null, now))
    }

    @Test
    fun metIsReadAtOpenMeteosHalfHourStepsInIndia() {
        val local13 = Instant.parse("2026-11-04T07:30:00Z")
        val local14 = Instant.parse("2026-11-04T08:30:00Z")
        val f =
            merger.merge(
                place,
                om(
                    ProviderHour(local13, temperature = 27.8),
                    ProviderHour(local14, temperature = 27.8),
                ),
                met(
                    ProviderHour(
                        h7,
                        temperature = 28.8,
                        condition = Condition.CLOUDY,
                        precipitation = 0.0,
                    ),
                    ProviderHour(
                        h8,
                        temperature = 28.0,
                        condition = Condition.RAIN,
                        precipitation = 1.2,
                    ),
                    ProviderHour(Instant.parse("2026-11-04T09:00:00Z"), temperature = 28.0),
                ),
                now,
            )!!
        assertEquals(listOf(local13, local14), f.hours.map { it.time })
        val first = f.hours.first()
        assertEquals(28.4, first.temperature!!.metNorway!!, 1e-9)
        assertEquals(Condition.CLOUDY, first.condition!!.metNorway)
        assertEquals(0.0, first.precipitation!!.metNorway!!, 1e-9)
        assertEquals(Condition.RAIN, f.hours[1].condition!!.metNorway)
    }

    @Test
    fun onlyWetAgainstDryCountsAsAConflict() {
        val f =
            merger.merge(
                place,
                om(
                    ProviderHour(h8, condition = Condition.DRIZZLE),
                    ProviderHour(h8 + java.time.Duration.ofHours(1), condition = Condition.CLOUDY),
                ),
                met(
                    ProviderHour(h8, condition = Condition.RAIN_SHOWERS),
                    ProviderHour(h8 + java.time.Duration.ofHours(1), condition = Condition.RAIN),
                ),
                now,
            )!!
        assertTrue(f.hours[0].condition!!.differs)
        assertFalse(f.hours[0].condition!!.conflicts)
        assertTrue(f.hours[1].condition!!.conflicts)
    }

    @Test
    fun pastHoursAreDroppedButTheCurrentHourStays() {
        val past = Instant.parse("2026-11-04T06:00:00Z")
        val f =
            merger.merge(
                place,
                om(ProviderHour(past), ProviderHour(h7), ProviderHour(h8)),
                null,
                now,
            )!!
        assertEquals(listOf(h7, h8), f.hours.map { it.time })
    }

    @Test
    fun uvPrefersOpenMeteoBecauseMetIsClearSky() {
        val f =
            merger.merge(
                place,
                om(ProviderHour(h8, uvIndex = 3.0)),
                met(ProviderHour(h8, uvIndex = 9.0)),
                now,
            )!!
        assertEquals(3.0, f.hours.single().uvIndex!!.value, 1e-9)
    }

    @Test
    fun windDirectionWrapsAroundNorth() {
        val d = ForecastMerger.circularMean(350.0, 10.0)!!.value
        assertTrue(d < 0.001 || d > 359.999)
        assertEquals(90.0, ForecastMerger.circularMean(80.0, 100.0)!!.value, 1e-9)
    }

    @Test
    fun currentUsesFreshObservationsOnly() {
        val stale = ProviderHour(Instant.parse("2026-11-04T03:00:00Z"), temperature = 20.0)
        val f =
            merger.merge(
                place,
                om(
                    ProviderHour(h8, temperature = 28.0),
                    current = ProviderHour(now, temperature = 29.0),
                ),
                met(stale),
                now,
            )!!
        assertEquals(29.0, f.current!!.temperature!!.value, 1e-9)
        assertNull(f.current!!.temperature!!.metNorway)
    }

    @Test
    fun yesterdayIsKeptApartAndRangesComeFromMet() {
        val yesterday = LocalDate.of(2026, 11, 3)
        val f =
            merger.merge(
                place,
                om(
                    ProviderHour(h8, temperature = 28.0, snowDepth = 0.0, cloudLow = 40.0),
                    days =
                        listOf(
                            ProviderDay(yesterday, temperatureMax = 27.0, sunshineSeconds = 3600.0)
                        ),
                ),
                met(
                    ProviderHour(
                        h8,
                        temperature = 26.0,
                        temperatureLow = 24.0,
                        temperatureHigh = 29.0,
                        cloudLow = 20.0,
                    )
                ),
                now,
                openMeteoModel = "ecmwf_ifs025",
            )!!
        assertEquals(yesterday, f.yesterday!!.date)
        assertTrue(f.days.isEmpty())
        val hour = f.hours.single()
        assertEquals(24.0, hour.temperatureLow!!, 1e-9)
        assertEquals(30.0, hour.cloudLow!!.value, 1e-9)
        assertEquals("ecmwf_ifs025", f.openMeteoModel)
    }

    @Test
    fun daysMergeAndStartToday() {
        val yesterday = LocalDate.of(2026, 11, 3)
        val today = LocalDate.of(2026, 11, 4)
        val f =
            merger.merge(
                place,
                om(
                    days =
                        listOf(
                            ProviderDay(yesterday, temperatureMax = 30.0),
                            ProviderDay(today, temperatureMax = 32.0, condition = Condition.CLEAR),
                        )
                ),
                met(
                    days =
                        listOf(
                            ProviderDay(today, temperatureMax = 30.0, condition = Condition.CLEAR)
                        )
                ),
                now,
            )!!
        assertEquals(listOf(today), f.days.map { it.date })
        assertEquals(31.0, f.days.single().temperatureMax!!.value, 1e-9)
        assertFalse(f.days.single().condition!!.differs)
    }
}

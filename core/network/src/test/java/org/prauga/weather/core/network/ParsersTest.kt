// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import java.time.Duration
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.model.Provider

class ParsersTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val oslo = ZoneId.of("Europe/Oslo")

    @Test
    fun openMeteoBengaluru() {
        val f = OpenMeteoParser.parse(Fixtures.read("om-blr"))
        assertEquals(Provider.OPEN_METEO, f.provider)
        assertEquals(16 * 24, f.hours.size)
        assertEquals(16, f.days.size)
        val current = f.current!!
        assertEquals(28.1, current.temperature!!, 0.001)
        assertNotNull(current.condition)
        assertTrue(f.hours.all { it.temperature != null && it.condition != null })
        assertTrue(f.hours.any { it.visibility != null })
        val today = f.days.first()
        assertTrue(today.temperatureMax!! >= today.temperatureMin!!)
        assertNotNull(today.sunrise)
        assertEquals(f.hours.first().time.atZone(kolkata).toLocalDate(), today.date)
    }

    @Test
    fun metBengaluruHasNoProbabilities() {
        val f = MetNorwayParser.parse(Fixtures.read("met-blr"), kolkata)
        assertEquals(Provider.MET_NORWAY, f.provider)
        assertTrue(f.hours.size in 40..70)
        f.hours.zipWithNext().forEach { (a, b) ->
            assertEquals(Duration.ofHours(1), Duration.between(a.time, b.time))
        }
        assertTrue(f.hours.all { it.temperature != null && it.condition != null })
        assertTrue(f.hours.all { it.precipitationProbability == null })
        assertTrue(f.days.size in 8..10)
        f.days.forEach { assertTrue(it.temperatureMax!! >= it.temperatureMin!!) }
        assertEquals(f.hours.first(), f.current)
    }

    @Test
    fun metOsloHasProbabilities() {
        val f = MetNorwayParser.parse(Fixtures.read("met-osl"), oslo)
        assertTrue(f.hours.any { it.precipitationProbability != null })
        assertTrue(f.hours.any { it.windGust != null })
        assertTrue(f.days.all { it.precipitationSum != null })
    }

    @Test
    fun metOsloHasRangesThunderAndCloudLayers() {
        val f = MetNorwayParser.parse(Fixtures.read("met-osl"), oslo)
        val hour = f.hours.first()
        assertTrue(
            hour.temperatureLow!! <= hour.temperature!! &&
                hour.temperature!! <= hour.temperatureHigh!!
        )
        assertTrue(hour.windLow != null && hour.windHigh != null)
        assertTrue(hour.thunderProbability != null && hour.precipitationMax != null)
        assertTrue(
            hour.cloudLow != null &&
                hour.cloudMid != null &&
                hour.cloudHigh != null &&
                hour.fog != null
        )
        assertTrue(f.days.first().precipitationMax != null)
    }

    @Test
    fun metOutsideTheNordicsHasNoRanges() {
        val f = MetNorwayParser.parse(Fixtures.read("met-blr"), kolkata)
        assertTrue(f.hours.all { it.temperatureLow == null && it.thunderProbability == null })
    }

    @Test
    fun metDropsPartlyCoveredLastDay() {
        val f = MetNorwayParser.parse(Fixtures.read("met-blr"), kolkata)
        val om = OpenMeteoParser.parse(Fixtures.read("om-blr"))
        assertTrue(f.days.last().date <= om.days.last().date)
        assertEquals(f.days.map { it.date }.distinct(), f.days.map { it.date })
    }
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.model.Pollen

class MoreSourcesTest {
    private val now = Instant.parse("2026-11-04T12:20:00Z")

    @Test
    fun moon() {
        val moon = MetMoreParser.moon(Fixtures.read("met-moon-osl"))!!
        assertEquals(Instant.parse("2026-11-04T00:14:00Z"), moon.rise)
        assertEquals(Instant.parse("2026-11-04T13:54:00Z"), moon.set)
        assertEquals(296.76, moon.phaseDegrees, 1e-9)
    }

    @Test
    fun metAirQualityPicksTheCurrentHour() {
        val air = MetMoreParser.airQuality(Fixtures.read("met-air-osl"), now)!!
        assertEquals(Instant.parse("2026-11-04T12:00:00Z"), air.time)
        assertTrue(air.index > 0 && air.pm25 != null && air.ozone != null)
        assertNotNull(air.area)
        assertNull(MetMoreParser.airQuality(Fixtures.read("met-air-osl"), now.plusSeconds(86_400)))
    }

    @Test
    fun ocean() {
        val ocean =
            MetMoreParser.ocean(
                Fixtures.read("met-ocean-bergen"),
                Instant.parse("2026-11-04T13:10:00Z"),
            )!!
        assertTrue(
            ocean.seaTemperature != null && ocean.waveHeight != null && ocean.currentSpeed != null
        )
    }

    @Test
    fun marine() {
        assertNotNull(OpenMeteoExtrasApi.parseMarine(Fixtures.read("om-marine-bergen")))
        val inland = """{"current":{"time":1,"wave_height":null,"sea_surface_temperature":null}}"""
        assertNull(OpenMeteoExtrasApi.parseMarine(inland))
    }

    @Test
    fun flood() {
        val river =
            OpenMeteoExtrasApi.parseFlood(
                Fixtures.read("om-flood-osl"),
                LocalDate.of(2026, 11, 4),
            )!!
        assertEquals(0.79, river.discharge, 1e-9)
        assertNotNull(river.mean)
        assertNull(
            OpenMeteoExtrasApi.parseFlood(Fixtures.read("om-flood-osl"), LocalDate.of(2027, 1, 1))
        )
    }

    @Test
    fun ensembleRange() {
        val hours = OpenMeteoExtrasApi.parseEnsemble(Fixtures.read("om-ensemble-osl"))
        assertEquals(72, hours.size)
        assertTrue(hours.all { it.low <= it.high && it.members >= 30 })
        assertEquals(1.0, OpenMeteoExtrasApi.percentile(listOf(0.0, 1.0, 2.0), 0.5), 1e-9)
    }

    @Test
    fun fullAirQuality() {
        val air = AirQualityApi.parse(Fixtures.read("om-air-osl"))!!
        assertNotNull(air.europeanAqi)
        assertNotNull(air.ozone)
        assertEquals(6, air.pollen.size)
        assertEquals(0.0, air.pollen.getValue(Pollen.BIRCH), 1e-9)
        assertTrue(AirQualityApi.parse(Fixtures.read("om-air-blr"))!!.pollen.isEmpty())
    }
}

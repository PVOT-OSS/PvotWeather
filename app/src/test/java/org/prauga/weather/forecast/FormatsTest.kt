// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.weather.R
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.core.model.WindUnit

class FormatsTest {
    @Test
    fun temperatures() {
        assertEquals(28, Formats.degrees(27.6))
        assertEquals(82, Formats.degrees(27.6, TemperatureUnit.FAHRENHEIT))
        assertEquals(-40, Formats.degrees(-40.0, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun windSpeeds() {
        assertEquals(36, Formats.wind(10.0))
        assertEquals(10, Formats.wind(10.0, WindUnit.METRES_PER_SECOND))
        assertEquals(22, Formats.wind(10.0, WindUnit.MILES_PER_HOUR))
        assertEquals(19, Formats.wind(10.0, WindUnit.KNOTS))
    }

    @Test
    fun compassPoints() {
        assertEquals(R.string.compass_n, Formats.compass(350.0))
        assertEquals(R.string.compass_ne, Formats.compass(45.0))
        assertEquals(R.string.compass_w, Formats.compass(-90.0))
    }

    @Test
    fun uvLevels() {
        assertEquals(R.string.uv_low, Formats.uvLevel(2.9))
        assertEquals(R.string.uv_high, Formats.uvLevel(7.0))
        assertEquals(R.string.uv_extreme, Formats.uvLevel(11.0))
    }

    @Test
    fun airQualityLevels() {
        assertEquals(R.string.aqi_good, Formats.aqiLevel(50))
        assertEquals(R.string.aqi_moderate, Formats.aqiLevel(84))
        assertEquals(R.string.aqi_hazardous, Formats.aqiLevel(301))
    }

    @Test
    fun usUnitsFollowFahrenheit() {
        assertEquals("0.50", Formats.precipitation(12.7, imperial = true))
        assertEquals("13", Formats.precipitation(12.7, imperial = false))
        assertEquals("29.92", Formats.pressure(1013.25, imperial = true))
        assertEquals("1013", Formats.pressure(1013.25, imperial = false))
        assertEquals("12", Formats.snowDepth(0.30, imperial = true))
        assertEquals("30", Formats.snowDepth(0.30, imperial = false))
        assertEquals("2.0", Formats.snowfall(5.08, imperial = true))
        assertEquals("3.3", Formats.waves(1.0, imperial = true))
        assertEquals("6.2", Formats.visibility(10_000.0, imperial = true))
        assertEquals("10", Formats.visibility(10_000.0, imperial = false))
        assertEquals("35", Formats.flow(1.0, imperial = true))
    }
}

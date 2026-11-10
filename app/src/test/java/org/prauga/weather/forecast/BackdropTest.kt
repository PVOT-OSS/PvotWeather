// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.compose.ui.graphics.Color
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.model.Condition

class BackdropTest {
    private val sunrise = Instant.parse("2026-11-04T00:44:00Z")
    private val sunset = Instant.parse("2026-11-04T12:22:00Z")
    private val noon = Instant.parse("2026-11-04T06:30:00Z")
    private val midnight = Instant.parse("2026-11-04T18:30:00Z")

    private fun of(condition: Condition?, isDay: Boolean? = true, now: Instant = noon) =
        Backdrop.of(condition, isDay, now, sunrise, sunset)

    @Test
    fun eachWeatherTypeHasItsPalette() {
        assertEquals(Backdrop.CLEAR, of(Condition.CLEAR))
        assertEquals(Backdrop.PARTLY_CLOUDY, of(Condition.PARTLY_CLOUDY))
        assertEquals(Backdrop.CLOUDY, of(Condition.CLOUDY))
        assertEquals(Backdrop.FOG, of(Condition.FOG))
        assertEquals(Backdrop.LIGHT_RAIN, of(Condition.DRIZZLE))
        assertEquals(Backdrop.LIGHT_RAIN, of(Condition.RAIN_SHOWERS))
        assertEquals(Backdrop.HEAVY_RAIN, of(Condition.HEAVY_RAIN))
        assertEquals(Backdrop.THUNDERSTORM, of(Condition.THUNDERSTORM))
        assertEquals(Backdrop.SNOW, of(Condition.SNOW_SHOWERS))
    }

    @Test
    fun clearSkiesFollowTheSun() {
        assertEquals(Backdrop.NIGHT, of(Condition.CLEAR, isDay = false, now = midnight))
        assertEquals(
            Backdrop.SUNRISE_SUNSET,
            of(Condition.CLEAR, now = sunset.minusSeconds(20 * 60)),
        )
        assertEquals(
            Backdrop.SUNRISE_SUNSET,
            of(Condition.PARTLY_CLOUDY, isDay = false, now = sunrise.minusSeconds(30 * 60)),
        )
        assertEquals(Backdrop.CLEAR, of(Condition.CLEAR, now = sunset.minusSeconds(60 * 60)))
    }

    @Test
    fun weatherBeatsTimeOfDay() {
        assertEquals(Backdrop.LIGHT_RAIN, of(Condition.RAIN, isDay = false, now = midnight))
        assertEquals(Backdrop.CLOUDY, of(Condition.CLOUDY, now = sunset))
    }

    @Test
    fun brightSkiesWantDarkStatusIcons() {
        assertTrue(Backdrop.CLOUDY.isLight(Color.Black, dark = true))
        assertTrue(Backdrop.SNOW.isLight(Color.Black, dark = true))
        assertTrue(Backdrop.FOG.isLight(Color.Black, dark = true))
        assertTrue(Backdrop.CLEAR.isLight(Color.White, dark = false))
        assertTrue(Backdrop.SUNRISE_SUNSET.isLight(Color.White, dark = false))
    }

    @Test
    fun darkSkiesWantLightStatusIcons() {
        assertFalse(Backdrop.NIGHT.isLight(Color.White, dark = false))
        assertFalse(Backdrop.THUNDERSTORM.isLight(Color.White, dark = false))
        assertFalse(Backdrop.HEAVY_RAIN.isLight(Color.Black, dark = true))
        assertFalse(Backdrop.CLEAR.isLight(Color.Black, dark = true))
    }

    @Test
    fun textOnTheSkyContrastsWithIt() {
        assertEquals(Color.White, Backdrop.NIGHT.onSky(Color.White, dark = false))
        assertEquals(Color.White, Backdrop.THUNDERSTORM.onSky(Color.Black, dark = true))
        assertNotEquals(Color.White, Backdrop.SNOW.onSky(Color.Black, dark = true))
        assertNotEquals(Color.White, Backdrop.CLOUDY.onSky(Color.Black, dark = true))
    }
}

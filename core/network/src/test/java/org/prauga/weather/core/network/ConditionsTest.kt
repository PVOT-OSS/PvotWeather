// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.prauga.weather.core.model.Condition

class ConditionsTest {
    @Test
    fun metSymbols() {
        assertEquals(Condition.CLEAR, Conditions.fromMetSymbol("clearsky_day"))
        assertEquals(Condition.CLEAR, Conditions.fromMetSymbol("fair_night"))
        assertEquals(Condition.PARTLY_CLOUDY, Conditions.fromMetSymbol("partlycloudy_day"))
        assertEquals(Condition.CLOUDY, Conditions.fromMetSymbol("cloudy"))
        assertEquals(Condition.DRIZZLE, Conditions.fromMetSymbol("lightrain"))
        assertEquals(Condition.RAIN, Conditions.fromMetSymbol("rain"))
        assertEquals(Condition.RAIN_SHOWERS, Conditions.fromMetSymbol("lightrainshowers_day"))
        assertEquals(Condition.HEAVY_RAIN, Conditions.fromMetSymbol("heavyrainshowers_night"))
        assertEquals(Condition.THUNDERSTORM, Conditions.fromMetSymbol("rainandthunder"))
        assertEquals(Condition.SLEET, Conditions.fromMetSymbol("lightsleetshowers_day"))
        assertEquals(Condition.SNOW_SHOWERS, Conditions.fromMetSymbol("snowshowers_polartwilight"))
        assertEquals(Condition.HEAVY_SNOW, Conditions.fromMetSymbol("heavysnow"))
        assertEquals(Condition.FOG, Conditions.fromMetSymbol("fog"))
        assertNull(Conditions.fromMetSymbol("nonsense"))
    }

    @Test
    fun metTimeOfDay() {
        assertEquals(true, Conditions.isDayFromMetSymbol("clearsky_day"))
        assertEquals(false, Conditions.isDayFromMetSymbol("clearsky_night"))
        assertEquals(false, Conditions.isDayFromMetSymbol("fair_polartwilight"))
        assertNull(Conditions.isDayFromMetSymbol("cloudy"))
    }

    @Test
    fun wmoCodes() {
        assertEquals(Condition.CLEAR, Conditions.fromWmo(0))
        assertEquals(Condition.PARTLY_CLOUDY, Conditions.fromWmo(2))
        assertEquals(Condition.CLOUDY, Conditions.fromWmo(3))
        assertEquals(Condition.FOG, Conditions.fromWmo(48))
        assertEquals(Condition.DRIZZLE, Conditions.fromWmo(53))
        assertEquals(Condition.HEAVY_RAIN, Conditions.fromWmo(82))
        assertEquals(Condition.THUNDERSTORM, Conditions.fromWmo(99))
        assertNull(Conditions.fromWmo(42))
    }
}

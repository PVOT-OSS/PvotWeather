// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {
    private val samples = (0 until 6000).map { it / 100f }

    @Test
    fun noiseIsStableAndUnit() {
        for (i in 0 until 500) {
            val n = SkyMath.noise(i, 3)
            assertEquals(n, SkyMath.noise(i, 3), 0f)
            assertTrue(n >= 0f && n < 1f)
        }
    }

    @Test
    fun lightningFlashesSometimesAndNeverSaturates() {
        val flashes = samples.map(SkyMath::flash)
        assertTrue(flashes.all { it in 0f..1f })
        assertTrue(flashes.count { it == 0f } > flashes.size * 0.8)
        assertTrue(flashes.any { it > 0.9f })
    }

    @Test
    fun meteorIsRareAndRunsStartToEnd() {
        val visible = samples.map(SkyMath::meteor).filter { it >= 0f }
        assertTrue(visible.size < samples.size * 0.15)
        assertTrue(visible.all { it <= 1f })
        assertTrue(visible.any { it < 0.1f } && visible.any { it > 0.9f })
    }

    @Test
    fun everyBackdropHasASky() {
        assertEquals(Backdrop.entries.size, Backdrop.entries.map { it.sky }.size)
        assertEquals(Sky.STORM, Backdrop.THUNDERSTORM.sky)
        assertEquals(Sky.STARS, Backdrop.NIGHT.sky)
    }
}

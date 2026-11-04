// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.model.DailyHigh
import org.prauga.weather.core.model.Place

class ExtrasRepositoryTest {
    @Test
    fun usualHighAveragesTheSameDaysOfPastYears() {
        val today = LocalDate.of(2026, 11, 4)
        val highs =
            (1..10).flatMap { y ->
                listOf(
                    DailyHigh(today.minusYears(y.toLong()), 28.0),
                    DailyHigh(today.minusYears(y.toLong()).plusDays(3), 26.0),
                    DailyHigh(today.minusYears(y.toLong()).plusDays(6), 99.0),
                )
            }
        assertEquals(27.0, ExtrasRepository.usualHigh(highs, today)!!, 1e-9)
    }

    @Test
    fun usualHighWrapsTheNewYear() {
        val today = LocalDate.of(2026, 1, 1)
        val highs = (1..5).map { DailyHigh(LocalDate.of(2025 - it, 12, 30), -3.0) }
        assertEquals(-3.0, ExtrasRepository.usualHigh(highs, today)!!, 1e-9)
    }

    @Test
    fun tooFewYearsGiveNoUsualHigh() {
        val today = LocalDate.of(2026, 11, 4)
        assertNull(ExtrasRepository.usualHigh(listOf(DailyHigh(today.minusYears(1), 20.0)), today))
    }

    @Test
    fun highsRoundTrip() {
        val highs =
            listOf(
                DailyHigh(LocalDate.of(2025, 11, 4), 27.1),
                DailyHigh(LocalDate.of(2024, 11, 4), 26.0),
            )
        assertEquals(highs, ExtrasRepository.decodeHighs(ExtrasRepository.encodeHighs(highs)))
    }

    @Test
    fun coverage() {
        val oslo = Place.of("Oslo", null, 59.91, 10.75, null, ZoneId.of("Europe/Oslo"))
        val stockholm =
            Place.of("Stockholm", null, 59.33, 18.07, null, ZoneId.of("Europe/Stockholm"))
        val bengaluru = Place.of("Bengaluru", null, 12.97, 77.59, null, ZoneId.of("Asia/Kolkata"))
        assertTrue(Coverage.hasNowcast(oslo) && Coverage.hasAlerts(oslo))
        assertTrue(Coverage.hasNowcast(stockholm))
        assertFalse(Coverage.hasNowcast(bengaluru) || Coverage.hasAlerts(bengaluru))
    }
}

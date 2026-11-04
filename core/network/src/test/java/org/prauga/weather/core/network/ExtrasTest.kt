// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.model.AlertLevel

class ExtrasTest {
    @Test
    fun nowcastHasFiveMinuteRates() {
        val nowcast = MetExtrasParser.nowcast(Fixtures.read("met-nowcast-osl"))!!
        assertTrue(nowcast.steps.size in 20..25)
        assertEquals(Instant.parse("2026-11-04T08:45:00Z"), nowcast.steps.first().time)
        assertTrue(nowcast.steps.all { it.rate >= 0 })
    }

    @Test
    fun nowcastWithoutRadarIsNull() {
        val body =
            """{"properties":{"meta":{"radar_coverage":"temporarily unavailable"},"timeseries":[]}}"""
        assertNull(MetExtrasParser.nowcast(body))
    }

    @Test
    fun alertsHaveLevelsAndEnds() {
        val alerts = MetExtrasParser.alerts(Fixtures.read("met-alerts"))
        assertEquals(2, alerts.size)
        assertTrue(
            alerts.all { it.level == AlertLevel.YELLOW && it.event.isNotEmpty() && it.ends != null }
        )
        assertTrue(
            MetExtrasParser.alerts("""{"features":[],"type":"FeatureCollection"}""").isEmpty()
        )
    }

    @Test
    fun archiveHighs() {
        val highs = ArchiveApi.parse(Fixtures.read("om-archive-blr"))
        assertEquals(14, highs.size)
        assertEquals(LocalDate.of(2025, 10, 28), highs.first().date)
        assertEquals(24.0, highs.first().temperatureMax, 1e-9)
    }

    @Test
    fun openMeteoMinutelyBecomesRates() {
        val body =
            """{"timezone":"UTC","minutely_15":{"time":[1793779200,1793780100],"precipitation":[0.0,0.5]}}"""
        val steps = OpenMeteoParser.parse(body).minutely
        assertEquals(2, steps.size)
        assertEquals(2.0, steps[1].rate, 1e-9)
    }
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.prauga.weather.core.model.PrecipitationStep

class RainOutlookTest {
    private val now = Instant.parse("2026-11-04T08:47:00Z")

    private fun steps(vararg rates: Double) =
        rates.mapIndexed { i, r ->
            PrecipitationStep(Instant.parse("2026-11-04T08:45:00Z").plusSeconds(i * 300L), r)
        }

    @Test
    fun dryStaysDry() = assertEquals(RainOutlook.Dry, RainOutlook.of(steps(0.0, 0.0, 0.0), now))

    @Test
    fun rainStartsInAboutTwentyFiveMinutes() =
        assertEquals(
            RainOutlook.Starting(25),
            RainOutlook.of(steps(0.0, 0.0, 0.0, 0.0, 0.0, 1.2), now),
        )

    @Test
    fun rainStops() =
        assertEquals(RainOutlook.Stopping(10), RainOutlook.of(steps(2.0, 1.0, 0.0), now))

    @Test
    fun rainContinues() =
        assertEquals(RainOutlook.Continuing, RainOutlook.of(steps(0.5, 0.6, 0.4), now))

    @Test fun tooLittleData() = assertNull(RainOutlook.of(steps(1.0), now))
}

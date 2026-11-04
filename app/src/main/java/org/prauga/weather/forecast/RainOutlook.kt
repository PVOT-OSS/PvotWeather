// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import org.prauga.weather.core.model.PrecipitationStep

sealed interface RainOutlook {
    data class Starting(val minutes: Int) : RainOutlook

    data class Stopping(val minutes: Int) : RainOutlook

    data object Continuing : RainOutlook

    data object Dry : RainOutlook

    companion object {
        private const val WET_MM_PER_HOUR = 0.1

        fun of(steps: List<PrecipitationStep>, now: Instant): RainOutlook? {
            val upcoming =
                steps.sortedBy { it.time }.dropWhile { it.time.isBefore(now.minusSeconds(5 * 60)) }
            if (upcoming.size < 2) return null
            val raining = upcoming.first().rate >= WET_MM_PER_HOUR
            val change = upcoming.firstOrNull { (it.rate >= WET_MM_PER_HOUR) != raining }
            return when {
                change == null -> if (raining) Continuing else Dry
                raining -> Stopping(minutesUntil(change.time, now))
                else -> Starting(minutesUntil(change.time, now))
            }
        }

        private fun minutesUntil(time: Instant, now: Instant): Int =
            ((Duration.between(now, time).toMinutes() / 5.0).roundToInt() * 5).coerceAtLeast(5)
    }
}

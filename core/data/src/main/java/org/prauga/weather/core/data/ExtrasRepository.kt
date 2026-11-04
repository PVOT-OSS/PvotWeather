// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.prauga.weather.core.data.cache.CachedForecast
import org.prauga.weather.core.data.cache.ForecastCacheDao
import org.prauga.weather.core.model.AirQuality
import org.prauga.weather.core.model.Alert
import org.prauga.weather.core.model.DailyHigh
import org.prauga.weather.core.model.Nowcast
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.network.AirQualityApi
import org.prauga.weather.core.network.ArchiveApi
import org.prauga.weather.core.network.Fetch
import org.prauga.weather.core.network.MetExtrasParser
import org.prauga.weather.core.network.MetNorwayApi

data class Extras(
    val nowcast: Nowcast? = null,
    val alerts: List<Alert> = emptyList(),
    val usualHigh: Double? = null,
    val airQuality: AirQuality? = null,
)

class ExtrasRepository(
    private val cache: ForecastCacheDao,
    private val metNorway: MetNorwayApi,
    private val archive: ArchiveApi,
    private val air: AirQualityApi,
    private val language: () -> String,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val retryAt = ConcurrentHashMap<Pair<String, String>, Instant>()

    fun observe(place: Place): Flow<Extras> =
        cache
            .observe(place.key)
            .map { rows ->
                val now = clock.instant()
                Extras(
                    nowcast =
                        rows
                            .body(NOWCAST)
                            ?.let { runCatching { MetExtrasParser.nowcast(it) }.getOrNull() }
                            ?.takeIf { n -> n.steps.any { it.time.isAfter(now) } },
                    alerts =
                        rows
                            .body(ALERTS)
                            ?.let { runCatching { MetExtrasParser.alerts(it) }.getOrNull() }
                            ?.filter { it.ends == null || it.ends!!.isAfter(now) }
                            .orEmpty(),
                    usualHigh =
                        rows.body(ARCHIVE)?.let {
                            usualHigh(decodeHighs(it), now.atZone(place.zone).toLocalDate())
                        },
                    airQuality =
                        rows
                            .body(AIR)
                            ?.let { runCatching { AirQualityApi.parse(it) }.getOrNull() }
                            ?.takeIf { Duration.between(it.time, now) < AIR_MAX_AGE },
                )
            }
            .flowOn(Dispatchers.Default)

    suspend fun refresh(place: Place) = coroutineScope {
        val jobs = buildList {
            if (Coverage.hasNowcast(place)) {
                add(
                    async {
                        guarded(place, NOWCAST) {
                            met(place, NOWCAST) { metNorway.nowcast(place, it) }
                        }
                    }
                )
            }
            if (Coverage.hasAlerts(place)) {
                add(
                    async {
                        guarded(place, ALERTS) {
                            met(place, ALERTS) { metNorway.alerts(place, language(), it) }
                        }
                    }
                )
            }
            add(async { guarded(place, ARCHIVE) { refreshArchive(place) } })
            add(async { guarded(place, AIR) { refreshAir(place) } })
        }
        jobs.forEach { it.await() }
    }

    private suspend fun met(place: Place, product: String, fetch: suspend (String?) -> Fetch) {
        val now = clock.instant()
        val row = cache.get(place.key, product)
        // MET's terms: never before Expires.
        if (row?.expiresAt != null && now.toEpochMilli() < row.expiresAt) return
        when (val result = fetch(row?.lastModified)) {
            is Fetch.NotModified ->
                cache.touch(place.key, product, now.toEpochMilli(), result.expires.orDefault(now))
            is Fetch.Body ->
                cache.upsert(
                    CachedForecast(
                        place.key,
                        product,
                        result.text,
                        now.toEpochMilli(),
                        result.expires.orDefault(now),
                        result.lastModified,
                    )
                )
        }
    }

    private suspend fun refreshArchive(place: Place) {
        val now = clock.instant()
        val row = cache.get(place.key, ARCHIVE)
        if (row?.expiresAt != null && now.toEpochMilli() < row.expiresAt) return
        val today = now.atZone(place.zone).toLocalDate()
        val highs =
            (1..YEARS).flatMap { years ->
                val day = today.minusYears(years.toLong())
                ArchiveApi.parse(archive.dailyHighs(place, day.minusDays(7), day.plusDays(6)))
            }
        if (highs.isEmpty()) return
        cache.upsert(
            CachedForecast(
                place.key,
                ARCHIVE,
                encodeHighs(highs),
                now.toEpochMilli(),
                (now + ARCHIVE_VALIDITY).toEpochMilli(),
                null,
            )
        )
    }

    private suspend fun refreshAir(place: Place) {
        val now = clock.instant()
        val row = cache.get(place.key, AIR)
        if (
            row != null && Duration.between(Instant.ofEpochMilli(row.fetchedAt), now) < AIR_INTERVAL
        )
            return
        val body = air.current(place)
        AirQualityApi.parse(body) ?: return
        cache.upsert(CachedForecast(place.key, AIR, body, now.toEpochMilli(), null, null))
    }

    private suspend fun guarded(place: Place, product: String, block: suspend () -> Unit) {
        val key = place.key to product
        retryAt[key]?.let { if (clock.instant().isBefore(it)) return }
        try {
            block()
            retryAt.remove(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            retryAt[key] = clock.instant() + RETRY
        }
    }

    private fun List<CachedForecast>.body(product: String) =
        firstOrNull { it.provider == product }?.body

    private fun Instant?.orDefault(now: Instant) =
        (this ?: now + Duration.ofMinutes(5)).toEpochMilli()

    companion object {
        const val NOWCAST = "MET_NOWCAST"
        const val ALERTS = "MET_ALERTS"
        const val ARCHIVE = "OPEN_METEO_ARCHIVE"
        const val AIR = "OPEN_METEO_AIR"
        private val AIR_INTERVAL: Duration = Duration.ofMinutes(60)
        private val AIR_MAX_AGE: Duration = Duration.ofHours(3)
        private const val YEARS = 10
        private val ARCHIVE_VALIDITY: Duration = Duration.ofDays(3)
        private val RETRY: Duration = Duration.ofMinutes(10)

        internal fun usualHigh(highs: List<DailyHigh>, today: LocalDate): Double? =
            highs
                .filter { high ->
                    (-1..1).any { shift ->
                        val sameYear = high.date.withYear(today.year + shift)
                        kotlin.math.abs(ChronoUnit.DAYS.between(today, sameYear)) <= 3
                    }
                }
                .map { it.temperatureMax }
                .takeIf { it.size >= 5 }
                ?.average()

        internal fun encodeHighs(highs: List<DailyHigh>) =
            highs.joinToString("\n") { "${it.date},${it.temperatureMax}" }

        internal fun decodeHighs(body: String) =
            body.lines().mapNotNull { line ->
                val (date, value) =
                    line.split(",").takeIf { it.size == 2 } ?: return@mapNotNull null
                runCatching { DailyHigh(LocalDate.parse(date), value.toDouble()) }.getOrNull()
            }
    }
}

object Coverage {
    fun hasNowcast(place: Place) = place.latitude in 54.5..71.5 && place.longitude in 4.0..31.6

    fun hasAlerts(place: Place) = place.latitude in 57.5..81.0 && place.longitude in 4.0..31.6
}

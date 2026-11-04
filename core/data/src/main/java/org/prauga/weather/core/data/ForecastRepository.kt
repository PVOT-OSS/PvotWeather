// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.prauga.weather.core.data.cache.CachedForecast
import org.prauga.weather.core.data.cache.ForecastCacheDao
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.model.ProviderForecast
import org.prauga.weather.core.network.Fetch
import org.prauga.weather.core.network.MetNorwayApi
import org.prauga.weather.core.network.MetNorwayParser
import org.prauga.weather.core.network.OpenMeteoApi
import org.prauga.weather.core.network.OpenMeteoParser
import org.prauga.weather.core.network.ProviderHttpException

data class ForecastSnapshot(val forecast: Forecast?, val fetchedAt: Map<Provider, Instant>)

sealed interface RefreshResult {
    data object Updated : RefreshResult

    data object NotDue : RefreshResult

    data class Failed(val throttled: Boolean, val message: String) : RefreshResult
}

class ForecastRepository(
    private val cache: ForecastCacheDao,
    private val openMeteo: OpenMeteoApi,
    private val metNorway: MetNorwayApi,
    private val merger: ForecastMerger = ForecastMerger(),
    private val clock: Clock = Clock.systemUTC(),
    private val tick: Duration = Duration.ofMinutes(5),
) {
    private val retryAt = ConcurrentHashMap<Pair<String, Provider>, Instant>()
    private val failures = ConcurrentHashMap<Pair<String, Provider>, Int>()

    fun observe(place: Place): Flow<ForecastSnapshot> =
        combine(cache.observe(place.key), ticker()) { rows, _ ->
                val om = rows.find(Provider.OPEN_METEO)
                val met = rows.find(Provider.MET_NORWAY)
                ForecastSnapshot(
                    forecast =
                        merger.merge(
                            place,
                            om?.let { parseOrNull(Provider.OPEN_METEO, it.body, place) },
                            met?.let { parseOrNull(Provider.MET_NORWAY, it.body, place) },
                            clock.instant(),
                        ),
                    fetchedAt =
                        rows
                            .mapNotNull { row ->
                                Provider.entries
                                    .find { it.name == row.provider }
                                    ?.let { it to Instant.ofEpochMilli(row.fetchedAt) }
                            }
                            .toMap(),
                )
            }
            .flowOn(Dispatchers.Default)

    suspend fun refresh(place: Place, force: Boolean = false): Map<Provider, RefreshResult> =
        coroutineScope {
            val om = async {
                guarded(place.key to Provider.OPEN_METEO) { refreshOpenMeteo(place, force) }
            }
            val met = async {
                guarded(place.key to Provider.MET_NORWAY) { refreshMetNorway(place) }
            }
            mapOf(Provider.OPEN_METEO to om.await(), Provider.MET_NORWAY to met.await())
        }

    private suspend fun refreshOpenMeteo(place: Place, force: Boolean): RefreshResult {
        val now = clock.instant()
        val row = cache.get(place.key, Provider.OPEN_METEO.name)
        if (row != null) {
            val age = Duration.between(Instant.ofEpochMilli(row.fetchedAt), now)
            val interval = if (force) OPEN_METEO_FORCED_INTERVAL else OPEN_METEO_INTERVAL
            if (age < interval) return RefreshResult.NotDue
        }
        val body = openMeteo.forecast(place)
        // Never replace a good cache with a body we can't read.
        OpenMeteoParser.parse(body.text)
        cache.upsert(
            CachedForecast(
                place.key,
                Provider.OPEN_METEO.name,
                body.text,
                now.toEpochMilli(),
                null,
                body.lastModified,
            )
        )
        return RefreshResult.Updated
    }

    private suspend fun refreshMetNorway(place: Place): RefreshResult {
        val now = clock.instant()
        val row = cache.get(place.key, Provider.MET_NORWAY.name)
        val expiresAt = row?.expiresAt?.let(Instant::ofEpochMilli)
        // MET's terms: never before Expires, even when forced.
        if (expiresAt != null && now.isBefore(expiresAt)) return RefreshResult.NotDue
        val fetchedAt = now.toEpochMilli()
        when (val fetch = metNorway.locationforecast(place, row?.lastModified)) {
            is Fetch.NotModified ->
                cache.touch(place.key, Provider.MET_NORWAY.name, fetchedAt, fetch.expiresOr(now))
            is Fetch.Body -> {
                MetNorwayParser.parse(fetch.text, place.zone)
                cache.upsert(
                    CachedForecast(
                        place.key,
                        Provider.MET_NORWAY.name,
                        fetch.text,
                        fetchedAt,
                        fetch.expires.or(now),
                        fetch.lastModified,
                    )
                )
            }
        }
        return RefreshResult.Updated
    }

    private suspend fun guarded(
        key: Pair<String, Provider>,
        block: suspend () -> RefreshResult,
    ): RefreshResult {
        val now = clock.instant()
        retryAt[key]?.let { if (now.isBefore(it)) return RefreshResult.NotDue }
        return try {
            block().also {
                failures.remove(key)
                retryAt.remove(key)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val n = failures.merge(key, 1, Int::plus) ?: 1
            val minutes = minOf(1L shl (n - 1).coerceAtMost(5), 30L)
            retryAt[key] =
                now + Duration.ofMinutes(minutes) + Duration.ofSeconds(Random.nextLong(30))
            RefreshResult.Failed(
                throttled = (e as? ProviderHttpException)?.isThrottled == true,
                message = e.message ?: e::class.java.simpleName,
            )
        }
    }

    private fun parseOrNull(provider: Provider, body: String, place: Place): ProviderForecast? =
        runCatching {
                when (provider) {
                    Provider.OPEN_METEO -> OpenMeteoParser.parse(body)
                    Provider.MET_NORWAY -> MetNorwayParser.parse(body, place.zone)
                }
            }
            .getOrNull()

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(tick.toMillis())
        }
    }

    private fun List<CachedForecast>.find(provider: Provider) = firstOrNull {
        it.provider == provider.name
    }

    private fun Fetch.NotModified.expiresOr(now: Instant) = expires.or(now)

    private fun Instant?.or(now: Instant): Long = (this ?: now + MET_DEFAULT_EXPIRY).toEpochMilli()

    companion object {
        val OPEN_METEO_INTERVAL: Duration = Duration.ofMinutes(30)
        val OPEN_METEO_FORCED_INTERVAL: Duration = Duration.ofMinutes(1)

        private val MET_DEFAULT_EXPIRY: Duration = Duration.ofMinutes(30)
    }
}

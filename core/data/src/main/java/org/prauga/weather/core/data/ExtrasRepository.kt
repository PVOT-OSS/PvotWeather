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
import org.prauga.weather.core.model.EnsembleHour
import org.prauga.weather.core.model.MetAirQuality
import org.prauga.weather.core.model.Moon
import org.prauga.weather.core.model.Nowcast
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.River
import org.prauga.weather.core.model.SeaState
import org.prauga.weather.core.network.AirQualityApi
import org.prauga.weather.core.network.ArchiveApi
import org.prauga.weather.core.network.Fetch
import org.prauga.weather.core.network.Marine
import org.prauga.weather.core.network.MetExtrasParser
import org.prauga.weather.core.network.MetMoreParser
import org.prauga.weather.core.network.MetNorwayApi
import org.prauga.weather.core.network.Ocean
import org.prauga.weather.core.network.OpenMeteoExtrasApi
import org.prauga.weather.core.network.ProviderHttpException

data class Extras(
    val nowcast: Nowcast? = null,
    val alerts: List<Alert> = emptyList(),
    val usualHigh: Double? = null,
    val airQuality: AirQuality? = null,
    val metAirQuality: MetAirQuality? = null,
    val moon: Moon? = null,
    val sea: SeaState? = null,
    val river: River? = null,
    val ensemble: List<EnsembleHour> = emptyList(),
)

class ExtrasRepository(
    private val cache: ForecastCacheDao,
    private val metNorway: MetNorwayApi,
    private val archive: ArchiveApi,
    private val air: AirQualityApi,
    private val openMeteoExtras: OpenMeteoExtrasApi,
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
                    metAirQuality =
                        rows.body(MET_AIR)?.let { b ->
                            runCatching { MetMoreParser.airQuality(b, now) }.getOrNull()
                        },
                    moon =
                        rows
                            .firstOrNull { it.provider == MOON }
                            ?.takeIf { fetchedOn(it, place) == today(place) }
                            ?.body
                            ?.let { runCatching { MetMoreParser.moon(it) }.getOrNull() },
                    sea =
                        sea(
                            rows.body(MARINE)?.let {
                                runCatching { OpenMeteoExtrasApi.parseMarine(it) }.getOrNull()
                            },
                            rows.body(OCEAN)?.let {
                                runCatching { MetMoreParser.ocean(it, now) }.getOrNull()
                            },
                        ),
                    river =
                        rows.body(FLOOD)?.let {
                            runCatching { OpenMeteoExtrasApi.parseFlood(it, today(place)) }
                                .getOrNull()
                        },
                    ensemble =
                        rows
                            .body(ENSEMBLE)
                            ?.let {
                                runCatching { OpenMeteoExtrasApi.parseEnsemble(it) }.getOrNull()
                            }
                            .orEmpty()
                            .filter { (it.time + Duration.ofHours(1)).isAfter(now) },
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
            add(
                async {
                    guarded(place, MOON) {
                        val date = today(place)
                        val offset =
                            place.zone.rules.getOffset(clock.instant()).id.let {
                                if (it == "Z") "+00:00" else it
                            }
                        val stale =
                            cache.get(place.key, MOON)?.let { fetchedOn(it, place) != date }
                                ?: false
                        met(place, MOON, { MetMoreParser.moon(it) }, refetch = stale) {
                            metNorway.moon(place, date, offset, it)
                        }
                    }
                }
            )
            if (Coverage.hasAlerts(place)) {
                add(
                    async {
                        guarded(place, MET_AIR) {
                            met(place, MET_AIR, { MetMoreParser.airQuality(it, clock.instant()) }) {
                                metNorway.airQuality(place, it)
                            }
                        }
                    }
                )
            }
            if (Coverage.hasOcean(place)) {
                add(
                    async {
                        guarded(place, OCEAN) {
                            met(place, OCEAN, { MetMoreParser.ocean(it, clock.instant()) }) {
                                metNorway.ocean(place, it)
                            }
                        }
                    }
                )
            }
            add(
                async {
                    guarded(place, MARINE) {
                        openMeteo(place, MARINE, Duration.ofMinutes(60)) {
                            openMeteoExtras.marine(place)
                        }
                    }
                }
            )
            add(
                async {
                    guarded(place, FLOOD) {
                        openMeteo(place, FLOOD, Duration.ofHours(6)) {
                            openMeteoExtras.flood(place)
                        }
                    }
                }
            )
            add(
                async {
                    guarded(place, ENSEMBLE) {
                        openMeteo(place, ENSEMBLE, Duration.ofMinutes(60)) {
                            openMeteoExtras.ensemble(place).also {
                                OpenMeteoExtrasApi.parseEnsemble(it)
                            }
                        }
                    }
                }
            )
        }
        jobs.forEach { it.await() }
    }

    private suspend fun met(
        place: Place,
        product: String,
        validate: (String) -> Unit = {},
        refetch: Boolean = false,
        fetch: suspend (String?) -> Fetch,
    ) {
        val now = clock.instant()
        val row = cache.get(place.key, product)
        // MET's terms: never before Expires.
        if (!refetch && row?.expiresAt != null && now.toEpochMilli() < row.expiresAt) return
        val result =
            try {
                fetch(row?.lastModified.takeUnless { refetch || row?.body.isNullOrEmpty() })
            } catch (e: ProviderHttpException) {
                if (e.status !in NOT_COVERED) throw e
                cache.upsert(
                    CachedForecast(
                        place.key,
                        product,
                        "",
                        now.toEpochMilli(),
                        (now + NOT_COVERED_FOR).toEpochMilli(),
                        null,
                    )
                )
                return
            }
        if (result is Fetch.Body) validate(result.text)
        when (result) {
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

    private suspend fun openMeteo(
        place: Place,
        product: String,
        interval: Duration,
        fetch: suspend () -> String,
    ) {
        val now = clock.instant()
        val row = cache.get(place.key, product)
        if (row != null && Duration.between(Instant.ofEpochMilli(row.fetchedAt), now) < interval)
            return
        cache.upsert(CachedForecast(place.key, product, fetch(), now.toEpochMilli(), null, null))
    }

    private fun today(place: Place): LocalDate = clock.instant().atZone(place.zone).toLocalDate()

    private fun fetchedOn(row: CachedForecast, place: Place): LocalDate =
        Instant.ofEpochMilli(row.fetchedAt).atZone(place.zone).toLocalDate()

    private fun List<CachedForecast>.body(product: String) =
        firstOrNull { it.provider == product }?.body?.takeIf { it.isNotEmpty() }

    private fun Instant?.orDefault(now: Instant) =
        (this ?: now + Duration.ofMinutes(5)).toEpochMilli()

    companion object {
        const val NOWCAST = "MET_NOWCAST"
        const val ALERTS = "MET_ALERTS"
        const val ARCHIVE = "OPEN_METEO_ARCHIVE"
        const val AIR = "OPEN_METEO_AIR"
        const val MET_AIR = "MET_AIR"
        const val MOON = "MET_MOON"
        const val OCEAN = "MET_OCEAN"
        const val MARINE = "OPEN_METEO_MARINE"
        const val FLOOD = "OPEN_METEO_FLOOD"
        const val ENSEMBLE = "OPEN_METEO_ENSEMBLE"
        private val NOT_COVERED = setOf(400, 404, 422)
        private val NOT_COVERED_FOR: Duration = Duration.ofDays(1)

        internal fun sea(marine: Marine?, ocean: Ocean?): SeaState? {
            if (marine == null && ocean == null) return null
            return SeaState(
                waveHeight = ForecastMerger.mean(marine?.waveHeight, ocean?.waveHeight),
                waveDirection = marine?.waveDirection ?: ocean?.waveDirection,
                wavePeriod = marine?.wavePeriod,
                seaTemperature = ForecastMerger.mean(marine?.seaTemperature, ocean?.seaTemperature),
                currentSpeed = ocean?.currentSpeed,
                currentDirection = ocean?.currentDirection,
            )
        }

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
    fun hasOcean(place: Place) = place.latitude in 54.0..82.0 && place.longitude in -10.0..40.0

    fun hasNowcast(place: Place) = place.latitude in 54.5..71.5 && place.longitude in 4.0..31.6

    fun hasAlerts(place: Place) = place.latitude in 57.5..81.0 && place.longitude in 4.0..31.6
}

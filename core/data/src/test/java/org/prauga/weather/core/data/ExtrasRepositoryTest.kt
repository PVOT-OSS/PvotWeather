// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.data.cache.CachedForecast
import org.prauga.weather.core.data.cache.ForecastCacheDao
import org.prauga.weather.core.model.DailyHigh
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.network.AirQualityApi
import org.prauga.weather.core.network.ArchiveApi
import org.prauga.weather.core.network.Marine
import org.prauga.weather.core.network.MetNorwayApi
import org.prauga.weather.core.network.Ocean
import org.prauga.weather.core.network.OpenMeteoExtrasApi
import org.prauga.weather.core.network.WeatherHttp

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

    @Test
    fun seaBlendsBothSources() {
        val sea =
            ExtrasRepository.sea(
                Marine(
                    waveHeight = 1.0,
                    waveDirection = 200.0,
                    wavePeriod = 6.0,
                    seaTemperature = 9.0,
                ),
                Ocean(Instant.EPOCH, 1.4, 210.0, 8.0, 0.3, 90.0),
            )!!
        assertEquals(1.2, sea.waveHeight!!.value, 1e-9)
        assertEquals(8.5, sea.seaTemperature!!.value, 1e-9)
        assertEquals(0.3, sea.currentSpeed!!, 1e-9)
        assertNull(ExtrasRepository.sea(null, null))
    }

    @Test
    fun notCoveredIsRememberedAndTruncatedBodiesAreNotCached() = runTest {
        val dao = MemoryDao()
        var oceanCalls = 0
        val engine = MockEngine { request ->
            when {
                "oceanforecast" in request.url.encodedPath -> {
                    oceanCalls++
                    respond("", HttpStatusCode.UnprocessableEntity)
                }
                "airqualityforecast" in request.url.encodedPath ->
                    respond("{\"data\":{\"time\":[{\"fr", HttpStatusCode.OK)
                "moon" in request.url.encodedPath ->
                    respond(
                        Fixtures.moon,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.Expires, "Wed, 04 Nov 2026 23:00:00 GMT"),
                    )
                else -> respond("{}", HttpStatusCode.OK)
            }
        }
        val client = WeatherHttp.client("PvotWeather/test (+test)", engine)
        val clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC)
        val repo =
            ExtrasRepository(
                dao,
                MetNorwayApi(client),
                ArchiveApi(client),
                AirQualityApi(client),
                OpenMeteoExtrasApi(client),
                { "en" },
                clock,
            )
        val oslo = Place.of("Oslo", null, 59.91, 10.75, null, ZoneId.of("Europe/Oslo"))
        repo.refresh(oslo)
        assertEquals("", dao.rows[oslo.key to ExtrasRepository.OCEAN]!!.body)
        assertNull(dao.rows[oslo.key to ExtrasRepository.MET_AIR])
        assertNotNull(dao.rows[oslo.key to ExtrasRepository.MOON])
        repo.refresh(oslo)
        assertEquals(1, oceanCalls)
    }

    private object Fixtures {
        const val moon =
            """{"properties":{"moonrise":{"time":"2026-11-04T01:14+01:00"},"moonset":{"time":"2026-11-04T14:54+01:00"},"moonphase":296.76}}"""
    }

    private class MemoryDao : ForecastCacheDao {
        val rows = linkedMapOf<Pair<String, String>, CachedForecast>()
        private val flow = MutableStateFlow(0)

        override fun observe(placeKey: String): Flow<List<CachedForecast>> =
            flow.map { rows.values.filter { it.placeKey == placeKey } }

        override suspend fun get(placeKey: String, provider: String) = rows[placeKey to provider]

        override suspend fun upsert(row: CachedForecast) {
            rows[row.placeKey to row.provider] = row
            flow.value++
        }

        override suspend fun touch(
            placeKey: String,
            provider: String,
            fetchedAt: Long,
            expiresAt: Long?,
        ) {
            rows[placeKey to provider]?.let {
                rows[placeKey to provider] = it.copy(fetchedAt = fetchedAt, expiresAt = expiresAt)
            }
        }

        override suspend fun delete(placeKey: String) {
            rows.keys.removeAll { it.first == placeKey }
        }
    }
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.data.cache.CachedForecast
import org.prauga.weather.core.data.cache.ForecastCacheDao
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.network.MetNorwayApi
import org.prauga.weather.core.network.OpenMeteoApi
import org.prauga.weather.core.network.WeatherHttp

class ForecastRepositoryTest {
    private val place =
        Place.of("Bengaluru", null, 12.9716, 77.5946, 920, ZoneId.of("Asia/Kolkata"))
    private var now = Instant.parse("2026-11-04T07:40:00Z")
    private val clock =
        object : Clock() {
            override fun instant() = now

            override fun getZone() = ZoneOffset.UTC

            override fun withZone(zone: ZoneId?) = this
        }
    private val dao = FakeDao()
    private val calls = mutableListOf<String>()
    private val models = mutableListOf<String?>()
    private var metStatus = HttpStatusCode.OK

    private val engine = MockEngine { request ->
        calls += request.url.host
        models += request.url.parameters["models"]
        when (request.url.host) {
            "api.open-meteo.com" -> respond(OM_BODY, HttpStatusCode.OK)
            else ->
                respond(
                    if (metStatus == HttpStatusCode.OK) MET_BODY else "",
                    metStatus,
                    headersOf(
                        HttpHeaders.Expires to listOf("Wed, 04 Nov 2026 08:08:05 GMT"),
                        HttpHeaders.LastModified to listOf("Wed, 04 Nov 2026 07:38:00 GMT"),
                    ),
                )
        }
    }
    private val client = WeatherHttp.client("PvotWeather/test (+test)", engine)
    private val repository =
        ForecastRepository(dao, OpenMeteoApi(client), MetNorwayApi(client), clock = clock)

    @Test
    fun firstRefreshAsksBothAndStoresMetHeaders() = runTest {
        val result = repository.refresh(place)
        assertEquals(RefreshResult.Updated, result[Provider.OPEN_METEO])
        assertEquals(RefreshResult.Updated, result[Provider.MET_NORWAY])
        val met = dao.rows.getValue(place.key to "MET_NORWAY")
        assertEquals(Instant.parse("2026-11-04T08:08:05Z").toEpochMilli(), met.expiresAt)
        assertEquals("Wed, 04 Nov 2026 07:38:00 GMT", met.lastModified)
    }

    @Test
    fun metIsNeverAskedBeforeExpiresEvenWhenForced() = runTest {
        repository.refresh(place)
        calls.clear()
        now += Duration.ofMinutes(5)
        val result = repository.refresh(place, force = true)
        assertEquals(RefreshResult.NotDue, result[Provider.MET_NORWAY])
        assertEquals(RefreshResult.Updated, result[Provider.OPEN_METEO])
        assertEquals(listOf("api.open-meteo.com"), calls)
    }

    @Test
    fun openMeteoWaitsThirtyMinutesUnlessForced() = runTest {
        repository.refresh(place)
        calls.clear()
        now += Duration.ofMinutes(10)
        assertEquals(RefreshResult.NotDue, repository.refresh(place)[Provider.OPEN_METEO])
        now += Duration.ofMinutes(25)
        assertEquals(RefreshResult.Updated, repository.refresh(place)[Provider.OPEN_METEO])
    }

    @Test
    fun notModifiedKeepsTheBodyAndMovesExpires() = runTest {
        repository.refresh(place)
        now = Instant.parse("2026-11-04T08:10:00Z")
        metStatus = HttpStatusCode.NotModified
        assertEquals(RefreshResult.Updated, repository.refresh(place)[Provider.MET_NORWAY])
        assertEquals(MET_BODY, dao.rows.getValue(place.key to "MET_NORWAY").body)
    }

    @Test
    fun throttlingFailsSoftlyAndBacksOff() = runTest {
        metStatus = HttpStatusCode.TooManyRequests
        val first = repository.refresh(place)[Provider.MET_NORWAY]
        assertTrue(first is RefreshResult.Failed && first.throttled)
        calls.clear()
        now += Duration.ofSeconds(20)
        assertEquals(RefreshResult.NotDue, repository.refresh(place)[Provider.MET_NORWAY])
        assertTrue("api.met.no" !in calls)
    }

    @Test
    fun aFailureForOnePlaceDoesNotHoldBackAnother() = runTest {
        metStatus = HttpStatusCode.TooManyRequests
        repository.refresh(place)
        metStatus = HttpStatusCode.OK
        val oslo = Place.of("Oslo", null, 59.9139, 10.7522, 23, ZoneId.of("Europe/Oslo"))
        assertEquals(RefreshResult.Updated, repository.refresh(oslo)[Provider.MET_NORWAY])
    }

    @Test
    fun backgroundRefreshCanSkipMet() = runTest {
        val result = repository.refresh(place, providers = setOf(Provider.OPEN_METEO))
        assertEquals(RefreshResult.NotDue, result[Provider.MET_NORWAY])
        assertEquals(listOf("api.open-meteo.com"), calls)
    }

    @Test
    fun independentSourcesUseEcmwfOnlyInTheNordics() = runTest {
        val independent =
            ForecastRepository(
                dao,
                OpenMeteoApi(client),
                MetNorwayApi(client),
                clock = clock,
                independentSources = flowOf(true),
            )
        val oslo = Place.of("Oslo", null, 59.91, 10.75, null, ZoneId.of("Europe/Oslo"))
        independent.refresh(oslo, providers = setOf(Provider.OPEN_METEO))
        independent.refresh(place, providers = setOf(Provider.OPEN_METEO))
        assertEquals(listOf("ecmwf_ifs025", null), models)
        assertTrue((oslo.key to "OPEN_METEO@ecmwf_ifs025") in dao.rows)
        assertTrue((place.key to "OPEN_METEO") in dao.rows)
    }

    private class FakeDao : ForecastCacheDao {
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
            flow.value++
        }

        override suspend fun delete(placeKey: String) {
            rows.keys.removeAll { it.first == placeKey }
            flow.value++
        }
    }

    private companion object {
        const val OM_BODY =
            """{"timezone":"Asia/Kolkata","hourly":{"time":[1793779200],"temperature_2m":[28.0]}}"""
        const val MET_BODY =
            """{"properties":{"timeseries":[{"time":"2026-11-04T08:00:00Z","data":{"instant":{"details":{"air_temperature":26.0}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}}]}}"""
    }
}

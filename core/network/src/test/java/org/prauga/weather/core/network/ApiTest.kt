// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.prauga.weather.core.model.Place

class ApiTest {
    private val place =
        Place.of("Bengaluru", null, 12.971598765, 77.594563123, 920, ZoneId.of("Asia/Kolkata"))
    private val agent = WeatherHttp.userAgent("1.0")

    @Test
    fun placeTruncatesToFourDecimals() {
        assertEquals("12.9715", Place.format(place.latitude))
        assertEquals("77.5945", Place.format(place.longitude))
        assertEquals("-33.8688", Place.format(Place.truncate(-33.86889)))
    }

    @Test
    fun metSendsIdentityAndConditionalRequest() = runTest {
        val engine = MockEngine { request ->
            assertEquals(agent, request.headers[HttpHeaders.UserAgent])
            assertTrue(agent.startsWith("PvotWeather/"))
            assertEquals("12.9715", request.url.parameters["lat"])
            assertEquals("77.5945", request.url.parameters["lon"])
            assertEquals("920", request.url.parameters["altitude"])
            assertEquals(
                "Wed, 04 Nov 2026 07:38:00 GMT",
                request.headers[HttpHeaders.IfModifiedSince],
            )
            respond(
                "",
                HttpStatusCode.NotModified,
                headersOf(HttpHeaders.Expires, "Wed, 04 Nov 2026 08:08:05 GMT"),
            )
        }
        val api = MetNorwayApi(WeatherHttp.client(agent, engine))
        val fetch = api.locationforecast(place, "Wed, 04 Nov 2026 07:38:00 GMT")
        assertEquals(Fetch.NotModified(Instant.parse("2026-11-04T08:08:05Z")), fetch)
    }

    @Test
    fun metKeepsCacheHeaders() = runTest {
        val engine = MockEngine { request ->
            assertNull(request.headers[HttpHeaders.IfModifiedSince])
            respond(
                "{}",
                HttpStatusCode.OK,
                headersOf(
                    HttpHeaders.Expires to listOf("Wed, 04 Nov 2026 08:08:05 GMT"),
                    HttpHeaders.LastModified to listOf("Wed, 04 Nov 2026 07:38:00 GMT"),
                ),
            )
        }
        val fetch = MetNorwayApi(WeatherHttp.client(agent, engine)).locationforecast(place, null)
        assertEquals(
            Fetch.Body(
                "{}",
                Instant.parse("2026-11-04T08:08:05Z"),
                "Wed, 04 Nov 2026 07:38:00 GMT",
            ),
            fetch,
        )
    }

    @Test
    fun throttlingIsReported() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.TooManyRequests) }
        try {
            MetNorwayApi(WeatherHttp.client(agent, engine)).locationforecast(place, null)
            fail()
        } catch (e: ProviderHttpException) {
            assertTrue(e.isThrottled)
        }
    }

    @Test
    fun openMeteoAsksForMetricUnixtimeLocalZone() = runTest {
        val engine = MockEngine { request ->
            assertEquals(agent, request.headers[HttpHeaders.UserAgent])
            val p = request.url.parameters
            assertEquals("12.9715", p["latitude"])
            assertEquals("ms", p["wind_speed_unit"])
            assertEquals("unixtime", p["timeformat"])
            assertEquals("auto", p["timezone"])
            assertEquals("16", p["forecast_days"])
            respond("{}", HttpStatusCode.OK)
        }
        assertEquals("{}", OpenMeteoApi(WeatherHttp.client(agent, engine)).forecast(place).text)
    }
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.model.Place

class GeocodingTest {
    @Test
    fun parsesAPlace() {
        val place = GeocodingApi.parse(Fixtures.read("geo-bengaluru")).first()
        assertEquals("Bengaluru", place.name)
        assertEquals("Karnataka", place.area)
        assertEquals("India", place.country)
        assertEquals(ZoneId.of("Asia/Kolkata"), place.zone)
        assertEquals(920, place.elevation)
        assertEquals("12.9719", Place.format(place.latitude))
    }

    @Test
    fun missingElevationIsNull() {
        val body =
            """{"results":[{"name":"Tromsø","latitude":69.6489,"longitude":18.95508,"elevation":-9999.0,"timezone":"Europe/Oslo","country":"Norway"}]}"""
        assertEquals(null, GeocodingApi.parse(body).single().elevation)
    }

    @Test
    fun noResultsIsAnEmptyList() {
        assertTrue(GeocodingApi.parse("""{"generationtime_ms":0.08}""").isEmpty())
    }

    @Test
    fun asksInTheUsersLanguage() = runTest {
        val engine = MockEngine { request ->
            assertEquals("PvotWeather/1.0 (+x)", request.headers[HttpHeaders.UserAgent])
            assertEquals("Oslo", request.url.parameters["name"])
            assertEquals("nb", request.url.parameters["language"])
            respond("{}", HttpStatusCode.OK)
        }
        val api = GeocodingApi(WeatherHttp.client("PvotWeather/1.0 (+x)", engine))
        assertTrue(api.search("Oslo", "nb").isEmpty())
    }
}

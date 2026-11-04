// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import org.prauga.weather.core.model.Place

class MetNorwayApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun locationforecast(place: Place, ifModifiedSince: String?): Fetch {
        val response =
            client.get("$baseUrl/weatherapi/locationforecast/2.0/complete") {
                parameter("lat", Place.format(place.latitude))
                parameter("lon", Place.format(place.longitude))
                place.elevation?.let { parameter("altitude", it) }
                ifModifiedSince?.let { header(HttpHeaders.IfModifiedSince, it) }
            }
        val expires = WeatherHttp.parseHttpDate(response.headers[HttpHeaders.Expires])
        return when {
            response.status == HttpStatusCode.NotModified -> Fetch.NotModified(expires)
            response.status.isSuccess() ->
                Fetch.Body(
                    response.bodyAsText(),
                    expires,
                    response.headers[HttpHeaders.LastModified],
                )
            else ->
                throw ProviderHttpException(response.status.value, "MET Norway: ${response.status}")
        }
    }

    companion object {
        const val BASE_URL = "https://api.met.no"
    }
}

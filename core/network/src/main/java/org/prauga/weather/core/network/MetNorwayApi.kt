// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import java.time.LocalDate
import org.prauga.weather.core.model.Place

class MetNorwayApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun locationforecast(place: Place, ifModifiedSince: String?): Fetch =
        fetch("locationforecast/2.0/complete", place, ifModifiedSince) {
            place.elevation?.let { parameter("altitude", it) }
        }

    suspend fun nowcast(place: Place, ifModifiedSince: String?): Fetch =
        fetch("nowcast/2.0/complete", place, ifModifiedSince)

    suspend fun alerts(place: Place, language: String, ifModifiedSince: String?): Fetch =
        fetch("metalerts/2.0/current.json", place, ifModifiedSince) {
            parameter(
                "lang",
                if (language == "nb" || language == "nn" || language == "no") "no" else "en",
            )
        }

    suspend fun moon(
        place: Place,
        date: LocalDate,
        offset: String,
        ifModifiedSince: String?,
    ): Fetch =
        fetch("sunrise/3.0/moon", place, ifModifiedSince) {
            parameter("date", date.toString())
            parameter("offset", offset)
        }

    suspend fun airQuality(place: Place, ifModifiedSince: String?): Fetch =
        fetch("airqualityforecast/0.1/", place, ifModifiedSince)

    suspend fun ocean(place: Place, ifModifiedSince: String?): Fetch =
        fetch("oceanforecast/2.0/complete", place, ifModifiedSince)

    private suspend fun fetch(
        path: String,
        place: Place,
        ifModifiedSince: String?,
        extra: HttpRequestBuilder.() -> Unit = {},
    ): Fetch {
        val response =
            client.get("$baseUrl/weatherapi/$path") {
                parameter("lat", Place.format(place.latitude))
                parameter("lon", Place.format(place.longitude))
                extra()
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

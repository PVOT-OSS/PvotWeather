// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import org.prauga.weather.core.model.Place

class OpenMeteoApi(private val client: HttpClient, private val baseUrl: String = BASE_URL) {

    suspend fun forecast(place: Place, model: String? = null): Fetch.Body {
        val response =
            client.get("$baseUrl/v1/forecast") {
                parameter("latitude", Place.format(place.latitude))
                parameter("longitude", Place.format(place.longitude))
                place.elevation?.let { parameter("elevation", it) }
                parameter("current", CURRENT)
                parameter("hourly", HOURLY)
                parameter("daily", DAILY)
                parameter("timezone", "auto")
                parameter("forecast_days", 16)
                parameter("past_days", 1)
                model?.let { parameter("models", it) }
                parameter("wind_speed_unit", "ms")
                parameter("timeformat", "unixtime")
                parameter("minutely_15", "precipitation")
                parameter("forecast_minutely_15", 8)
            }
        if (!response.status.isSuccess()) {
            throw ProviderHttpException(response.status.value, "Open-Meteo: ${response.status}")
        }
        return Fetch.Body(
            text = response.bodyAsText(),
            expires = null,
            lastModified = response.headers[HttpHeaders.LastModified],
        )
    }

    companion object {
        const val BASE_URL = "https://api.open-meteo.com"

        private const val CURRENT =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation," +
                "weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m"
        private const val HOURLY =
            "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature," +
                "precipitation_probability,precipitation,weather_code,pressure_msl,cloud_cover," +
                "visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,is_day," +
                "cloud_cover_low,cloud_cover_mid,cloud_cover_high,snowfall,snow_depth"
        private const val DAILY =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
                "precipitation_sum,precipitation_probability_max,snowfall_sum,sunshine_duration," +
                "daylight_duration"
    }
}

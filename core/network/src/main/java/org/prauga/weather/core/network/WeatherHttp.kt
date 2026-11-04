// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object WeatherHttp {
    const val WEBSITE = "https://github.com/PVOT-OSS/PvotWeather"

    fun userAgent(versionName: String) = "PvotWeather/$versionName (+$WEBSITE)"

    fun client(userAgent: String, engine: HttpClientEngine? = null): HttpClient {
        val configure: HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(UserAgent) { agent = userAgent }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
            }
        }
        return if (engine != null) HttpClient(engine, configure) else HttpClient(Android, configure)
    }

    internal fun parseHttpDate(value: String?): Instant? =
        value?.let {
            runCatching {
                    ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                }
                .getOrNull()
        }
}

sealed interface Fetch {
    data class Body(val text: String, val expires: Instant?, val lastModified: String?) : Fetch

    data class NotModified(val expires: Instant?) : Fetch
}

class ProviderHttpException(val status: Int, message: String) : Exception(message) {
    val isThrottled: Boolean
        get() = status == 429
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

import kotlinx.serialization.json.Json

internal val WeatherJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

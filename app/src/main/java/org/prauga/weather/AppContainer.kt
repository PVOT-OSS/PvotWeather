// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather

import android.content.Context
import java.time.ZoneId
import org.prauga.weather.core.data.ForecastRepository
import org.prauga.weather.core.data.cache.WeatherDatabase
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.network.MetNorwayApi
import org.prauga.weather.core.network.OpenMeteoApi
import org.prauga.weather.core.network.WeatherHttp

class AppContainer(context: Context) {
    private val http = WeatherHttp.client(WeatherHttp.userAgent(BuildConfig.VERSION_NAME))
    private val database = WeatherDatabase.create(context)

    val forecasts =
        ForecastRepository(database.forecastCache(), OpenMeteoApi(http), MetNorwayApi(http))

    val defaultPlace =
        Place.of("Bengaluru", "Karnataka", 12.9716, 77.5946, 920, ZoneId.of("Asia/Kolkata"))
}

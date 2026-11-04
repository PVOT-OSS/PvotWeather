// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather

import android.content.Context
import org.prauga.weather.core.data.ForecastRepository
import org.prauga.weather.core.data.PlaceRepository
import org.prauga.weather.core.data.WeatherPreferences
import org.prauga.weather.core.data.cache.WeatherDatabase
import org.prauga.weather.core.network.GeocodingApi
import org.prauga.weather.core.network.MetNorwayApi
import org.prauga.weather.core.network.OpenMeteoApi
import org.prauga.weather.core.network.WeatherHttp
import org.prauga.weather.location.LocateCurrentPlace
import org.prauga.weather.location.LocationFinder

class AppContainer(context: Context) {
    private val http = WeatherHttp.client(WeatherHttp.userAgent(BuildConfig.VERSION_NAME))
    private val database = WeatherDatabase.create(context)

    val forecasts =
        ForecastRepository(database.forecastCache(), OpenMeteoApi(http), MetNorwayApi(http))
    val places = PlaceRepository(database.places(), database.forecastCache())
    val preferences = WeatherPreferences(context)
    val geocoding = GeocodingApi(http)
    val locateCurrentPlace = LocateCurrentPlace(LocationFinder(context), places)
}

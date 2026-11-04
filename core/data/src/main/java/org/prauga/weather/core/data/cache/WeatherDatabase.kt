// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CachedForecast::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun forecastCache(): ForecastCacheDao

    companion object {
        fun create(context: Context): WeatherDatabase =
            Room.databaseBuilder(context, WeatherDatabase::class.java, "weather.db").build()
    }
}

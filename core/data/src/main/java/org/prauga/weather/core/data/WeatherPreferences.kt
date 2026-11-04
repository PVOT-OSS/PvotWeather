// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.core.model.WindUnit

class WeatherPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)

    val selectedPlaceId: Flow<Long?> = observe { getLong(SELECTED_PLACE, 0).takeIf { it != 0L } }

    val temperatureUnit: Flow<TemperatureUnit> = observe {
        enumOr(TEMPERATURE_UNIT, TemperatureUnit.CELSIUS)
    }

    val windUnit: Flow<WindUnit> = observe { enumOr(WIND_UNIT, WindUnit.KILOMETRES_PER_HOUR) }

    fun selectPlace(id: Long) = prefs.edit().putLong(SELECTED_PLACE, id).apply()

    fun setTemperatureUnit(unit: TemperatureUnit) =
        prefs.edit().putString(TEMPERATURE_UNIT, unit.name).apply()

    fun setWindUnit(unit: WindUnit) = prefs.edit().putString(WIND_UNIT, unit.name).apply()

    private inline fun <reified E : Enum<E>> SharedPreferences.enumOr(key: String, default: E): E =
        getString(key, null)?.let { name -> enumValues<E>().find { it.name == name } } ?: default

    private fun <T> observe(read: SharedPreferences.() -> T): Flow<T> =
        callbackFlow {
                trySend(prefs.read())
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> trySend(p.read()) }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            .conflate()

    private companion object {
        const val SELECTED_PLACE = "selected_place"
        const val TEMPERATURE_UNIT = "temperature_unit"
        const val WIND_UNIT = "wind_unit"
    }
}

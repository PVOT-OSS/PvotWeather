// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.weather.core.data.ForecastRepository
import org.prauga.weather.core.data.PlaceRepository
import org.prauga.weather.core.data.WeatherPreferences
import org.prauga.weather.core.model.Forecast

@OptIn(ExperimentalCoroutinesApi::class)
class CompareViewModel(
    forecasts: ForecastRepository,
    places: PlaceRepository,
    preferences: WeatherPreferences,
) : ViewModel() {

    val forecast: StateFlow<Forecast?> =
        combine(places.observeAll(), preferences.selectedPlaceId) { all, id ->
                all.find { it.id == id } ?: all.firstOrNull()
            }
            .distinctUntilChanged()
            .flatMapLatest { place ->
                place?.let { p -> forecasts.observe(p).map { it.forecast } } ?: flowOf(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

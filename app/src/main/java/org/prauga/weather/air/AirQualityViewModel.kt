// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.air

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.prauga.weather.core.data.Extras
import org.prauga.weather.core.data.ExtrasRepository
import org.prauga.weather.core.data.PlaceRepository
import org.prauga.weather.core.data.WeatherPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class AirQualityViewModel(
    extras: ExtrasRepository,
    places: PlaceRepository,
    preferences: WeatherPreferences,
) : ViewModel() {
    val extras: StateFlow<Extras?> =
        combine(places.observeAll(), preferences.selectedPlaceId) { all, id ->
                all.find { it.id == id } ?: all.firstOrNull()
            }
            .distinctUntilChanged()
            .flatMapLatest { place -> place?.let { extras.observe(it) } ?: flowOf(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.weather.core.data.Extras
import org.prauga.weather.core.data.ExtrasRepository
import org.prauga.weather.core.data.ForecastRepository
import org.prauga.weather.core.data.PlaceRepository
import org.prauga.weather.core.data.RefreshResult
import org.prauga.weather.core.data.WeatherPreferences
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.Provider
import org.prauga.weather.location.LocateCurrentPlace

data class ForecastUiState(
    val place: Place? = null,
    val forecast: Forecast? = null,
    val fetchedAt: Map<Provider, Instant> = emptyMap(),
    val loading: Boolean = true,
    val noPlaces: Boolean = false,
    val refreshing: Boolean = false,
    val failed: Set<Provider> = emptySet(),
    val extras: Extras = Extras(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModel(
    private val forecasts: ForecastRepository,
    private val extras: ExtrasRepository,
    places: PlaceRepository,
    preferences: WeatherPreferences,
    private val locateCurrentPlace: LocateCurrentPlace,
) : ViewModel() {

    private data class Selection(val place: Place?, val loaded: Boolean)

    private data class RefreshState(
        val refreshing: Boolean = false,
        val failed: Set<Provider> = emptySet(),
    )

    private val selection: StateFlow<Selection> =
        combine(places.observeAll(), preferences.selectedPlaceId) { all, id ->
                Selection(all.find { it.id == id } ?: all.firstOrNull(), loaded = true)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, Selection(null, loaded = false))

    private val refresh = MutableStateFlow(RefreshState())

    val state: StateFlow<ForecastUiState> =
        selection
            .flatMapLatest { (place, loaded) ->
                if (place == null) {
                    flowOf(ForecastUiState(loading = !loaded, noPlaces = loaded))
                } else {
                    combine(forecasts.observe(place), extras.observe(place), refresh) {
                        snapshot,
                        more,
                        r ->
                        ForecastUiState(
                            place = place,
                            forecast = snapshot.forecast,
                            fetchedAt = snapshot.fetchedAt,
                            loading = false,
                            refreshing = r.refreshing,
                            failed = r.failed,
                            extras = more,
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState())

    fun refresh(force: Boolean = false) {
        if (refresh.value.refreshing) return
        viewModelScope.launch {
            var place = selection.first { it.loaded }.place ?: return@launch
            refresh.update { it.copy(refreshing = true) }
            if (place.isCurrentLocation && locateCurrentPlace.hasPermission) {
                locateCurrentPlace()?.let { place = it }
            }
            val extrasJob = launch { extras.refresh(place) }
            val results = forecasts.refresh(place, force)
            extrasJob.join()
            refresh.update { state ->
                RefreshState(
                    refreshing = false,
                    failed =
                        Provider.entries
                            .filter { p ->
                                when (results[p]) {
                                    is RefreshResult.Failed -> true
                                    RefreshResult.Updated -> false
                                    else -> p in state.failed
                                }
                            }
                            .toSet(),
                )
            }
        }
    }
}

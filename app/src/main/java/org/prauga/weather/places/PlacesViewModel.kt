// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.weather.core.data.PlaceRepository
import org.prauga.weather.core.data.WeatherPreferences
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.network.GeocodingApi
import org.prauga.weather.location.LocateCurrentPlace

sealed interface SearchState {
    data object Idle : SearchState

    data object Searching : SearchState

    data class Results(val places: List<Place>) : SearchState

    data object Failed : SearchState
}

data class PlacesUiState(
    val query: String = "",
    val search: SearchState = SearchState.Idle,
    val saved: List<Place> = emptyList(),
    val selectedId: Long? = null,
    val locating: Boolean = false,
    val locateFailed: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PlacesViewModel(
    private val places: PlaceRepository,
    private val geocoding: GeocodingApi,
    private val preferences: WeatherPreferences,
    private val locateCurrentPlace: LocateCurrentPlace,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val locating = MutableStateFlow(Locating())
    private val done = Channel<Unit>(Channel.CONFLATED)

    val closes: Flow<Unit> = done.receiveAsFlow()

    val hasLocationPermission: Boolean
        get() = locateCurrentPlace.hasPermission

    private val search: Flow<SearchState> =
        query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(300)
            .flatMapLatest { q ->
                flow {
                    if (q.length < 2) {
                        emit(SearchState.Idle)
                        return@flow
                    }
                    emit(SearchState.Searching)
                    emit(
                        try {
                            SearchState.Results(geocoding.search(q, Locale.getDefault().language))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            SearchState.Failed
                        }
                    )
                }
            }

    val state: StateFlow<PlacesUiState> =
        combine(query, search, places.observeAll(), preferences.selectedPlaceId, locating) {
                q,
                s,
                saved,
                selected,
                l ->
                PlacesUiState(
                    query = q,
                    search = if (q.isBlank()) SearchState.Idle else s,
                    saved = saved,
                    selectedId = selected ?: saved.firstOrNull()?.id,
                    locating = l.running,
                    locateFailed = l.failed,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacesUiState())

    fun onQueryChange(value: String) = query.update { value }

    fun add(place: Place) {
        viewModelScope.launch {
            preferences.selectPlace(places.add(place))
            done.send(Unit)
        }
    }

    fun select(place: Place) {
        preferences.selectPlace(place.id)
        done.trySend(Unit)
    }

    fun remove(place: Place) {
        viewModelScope.launch { places.remove(place.id) }
    }

    fun useMyLocation() {
        if (locating.value.running) return
        viewModelScope.launch {
            locating.value = Locating(running = true)
            val place = locateCurrentPlace()
            locating.value = Locating(failed = place == null)
            if (place != null) select(place)
        }
    }

    fun locationDenied() {
        locating.value = Locating(failed = true)
    }

    private data class Locating(val running: Boolean = false, val failed: Boolean = false)
}

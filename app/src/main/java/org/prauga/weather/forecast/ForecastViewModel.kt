// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.weather.core.data.ForecastRepository
import org.prauga.weather.core.data.RefreshResult
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.Provider

data class ForecastUiState(
    val place: Place,
    val forecast: Forecast? = null,
    val fetchedAt: Map<Provider, Instant> = emptyMap(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val failed: Set<Provider> = emptySet(),
)

class ForecastViewModel(private val repository: ForecastRepository, private val place: Place) :
    ViewModel() {

    private data class RefreshState(
        val refreshing: Boolean = false,
        val failed: Set<Provider> = emptySet(),
    )

    private val refresh = MutableStateFlow(RefreshState())

    val state: StateFlow<ForecastUiState> =
        combine(repository.observe(place), refresh) { snapshot, r ->
                ForecastUiState(
                    place = place,
                    forecast = snapshot.forecast,
                    fetchedAt = snapshot.fetchedAt,
                    loading = false,
                    refreshing = r.refreshing,
                    failed = r.failed,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState(place))

    fun refresh(force: Boolean = false) {
        if (refresh.value.refreshing) return
        viewModelScope.launch {
            refresh.update { it.copy(refreshing = true) }
            val results = repository.refresh(place, force)
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

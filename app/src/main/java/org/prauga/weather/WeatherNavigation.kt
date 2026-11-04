// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import org.prauga.weather.forecast.ForecastRoute
import org.prauga.weather.forecast.ForecastViewModel
import org.prauga.weather.places.PlacesScreen
import org.prauga.weather.places.PlacesViewModel

@Serializable data object ForecastKey : NavKey

@Serializable data object PlacesKey : NavKey

@Composable
fun WeatherNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(ForecastKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<ForecastKey> {
                    ForecastRoute(
                        viewModel =
                            viewModel {
                                ForecastViewModel(
                                    container.forecasts,
                                    container.places,
                                    container.preferences,
                                    container.locateCurrentPlace,
                                )
                            },
                        onOpenPlaces = { backStack.add(PlacesKey) },
                    )
                }
                entry<PlacesKey> {
                    PlacesScreen(
                        viewModel =
                            viewModel {
                                PlacesViewModel(
                                    container.places,
                                    container.geocoding,
                                    container.preferences,
                                    container.locateCurrentPlace,
                                )
                            },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            },
    )
}

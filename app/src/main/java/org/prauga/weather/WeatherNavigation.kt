// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import org.prauga.weather.about.AboutScreen
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.core.model.WindUnit
import org.prauga.weather.forecast.ForecastRoute
import org.prauga.weather.forecast.ForecastViewModel
import org.prauga.weather.forecast.MenuItem
import org.prauga.weather.places.PlacesScreen
import org.prauga.weather.places.PlacesViewModel
import org.prauga.weather.settings.LocalUnits
import org.prauga.weather.settings.SettingsScreen
import org.prauga.weather.settings.Units

@Serializable data object ForecastKey : NavKey

@Serializable data object PlacesKey : NavKey

@Serializable data object SettingsKey : NavKey

@Serializable data object AboutKey : NavKey

@Composable
fun WeatherNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(ForecastKey)
    val temperature by
        container.preferences.temperatureUnit.collectAsStateWithLifecycle(TemperatureUnit.CELSIUS)
    val wind by
        container.preferences.windUnit.collectAsStateWithLifecycle(WindUnit.KILOMETRES_PER_HOUR)
    CompositionLocalProvider(LocalUnits provides Units(temperature, wind)) {
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
                                        container.extras,
                                        container.places,
                                        container.preferences,
                                        container.locateCurrentPlace,
                                    )
                                },
                            onOpenPlaces = { backStack.add(PlacesKey) },
                            menu =
                                listOf(
                                    MenuItem(R.string.settings) { backStack.add(SettingsKey) },
                                    MenuItem(R.string.about) { backStack.add(AboutKey) },
                                ),
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
                    entry<AboutKey> { AboutScreen(onBack = { backStack.removeLastOrNull() }) }
                    entry<SettingsKey> {
                        SettingsScreen(
                            container.preferences,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
        )
    }
}

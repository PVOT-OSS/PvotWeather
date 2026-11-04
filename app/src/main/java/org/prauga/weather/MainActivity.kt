// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.weather.forecast.ForecastRoute
import org.prauga.weather.forecast.ForecastViewModel

class MainActivity : PvotBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as WeatherApplication).container
        setPvotContent {
            PvotAppTheme {
                ForecastRoute(
                    viewModel(
                        factory =
                            viewModelFactory {
                                initializer {
                                    ForecastViewModel(container.forecasts, container.defaultPlace)
                                }
                            }
                    )
                )
            }
        }
    }
}

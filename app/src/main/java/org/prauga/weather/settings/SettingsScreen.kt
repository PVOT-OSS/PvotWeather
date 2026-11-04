// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import org.prauga.weather.R
import org.prauga.weather.core.data.WeatherPreferences
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.core.model.WindUnit

@Composable
fun SettingsScreen(preferences: WeatherPreferences, onBack: () -> Unit) {
    val temperature by
        preferences.temperatureUnit.collectAsStateWithLifecycle(TemperatureUnit.CELSIUS)
    val wind by preferences.windUnit.collectAsStateWithLifecycle(WindUnit.KILOMETRES_PER_HOUR)
    PvotScreenScaffold(
        topBar = { PvotTopBar(stringResource(R.string.settings), onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 32.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                PvotListGroup(title = stringResource(R.string.temperature)) {
                    TemperatureUnit.entries.forEach { unit ->
                        item {
                            Choice(stringResource(unit.label), unit == temperature) {
                                preferences.setTemperatureUnit(unit)
                            }
                        }
                    }
                }
            }
            item {
                PvotListGroup(title = stringResource(R.string.wind_speed_unit)) {
                    WindUnit.entries.forEach { unit ->
                        item {
                            Choice(stringResource(unit.symbol), unit == wind) {
                                preferences.setWindUnit(unit)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Choice(title: String, selected: Boolean, onClick: () -> Unit) {
    PvotListRow(
        title = title,
        trailing =
            if (selected) {
                { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
            } else null,
        selected = selected,
        onClick = onClick,
    )
}

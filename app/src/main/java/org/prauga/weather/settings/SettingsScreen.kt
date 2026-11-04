// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import org.prauga.weather.R
import org.prauga.weather.SubScreen
import org.prauga.weather.core.data.WeatherPreferences
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.core.model.WindUnit

@Composable
fun SettingsScreen(preferences: WeatherPreferences, onBack: () -> Unit) {
    val temperature by
        preferences.temperatureUnit.collectAsStateWithLifecycle(TemperatureUnit.CELSIUS)
    val wind by preferences.windUnit.collectAsStateWithLifecycle(WindUnit.KILOMETRES_PER_HOUR)
    val independent by preferences.independentSources.collectAsStateWithLifecycle(false)
    SubScreen(stringResource(R.string.settings), onBack, Arrangement.spacedBy(20.dp)) {
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
            Text(
                stringResource(R.string.units_follow),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        item {
            PvotListGroup(title = stringResource(R.string.sources)) {
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.independent_sources),
                        summary = stringResource(R.string.independent_sources_summary),
                        checked = independent,
                        onCheckedChange = preferences::setIndependentSources,
                    )
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

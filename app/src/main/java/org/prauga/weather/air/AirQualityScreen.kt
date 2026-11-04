// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.air

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListGroupScope
import com.prauga.pvot.designsystem.components.list.PvotListRow
import java.util.Locale
import kotlin.math.roundToInt
import org.prauga.weather.R
import org.prauga.weather.SubScreen
import org.prauga.weather.core.model.Pollen
import org.prauga.weather.forecast.Formats

@Composable
fun AirQualityScreen(viewModel: AirQualityViewModel, onBack: () -> Unit) {
    val extras by viewModel.extras.collectAsStateWithLifecycle()
    val air = extras?.airQuality
    val met = extras?.metAirQuality
    SubScreen(stringResource(R.string.air_quality_title), onBack, Arrangement.spacedBy(20.dp)) {
        if (air == null && met == null) {
            item { Note(stringResource(R.string.air_none)) }
            return@SubScreen
        }
        air?.let { a ->
            item {
                PvotListGroup(title = stringResource(R.string.air_quality_open_meteo)) {
                    row(R.string.air_us_aqi, "${a.usAqi}", Formats.aqiLevel(a.usAqi))
                    a.europeanAqi?.let {
                        row(R.string.air_european_aqi, "$it", Formats.europeanAqiLevel(it))
                    }
                    concentration(R.string.air_pm25_name, a.pm25)
                    concentration(R.string.air_pm10_name, a.pm10)
                    concentration(R.string.air_ozone, a.ozone)
                    concentration(R.string.air_no2, a.nitrogenDioxide)
                    concentration(R.string.air_so2, a.sulphurDioxide)
                    concentration(R.string.air_co, a.carbonMonoxide)
                }
            }
            item {
                if (a.pollen.isEmpty()) {
                    Note(stringResource(R.string.pollen_europe_only))
                } else {
                    PvotListGroup(title = stringResource(R.string.pollen_title)) {
                        Pollen.entries.forEach { p ->
                            a.pollen[p]?.let { level ->
                                item {
                                    PvotListRow(
                                        title = stringResource(Formats.pollenName(p)),
                                        trailing = {
                                            Text(
                                                pluralStringResource(
                                                    R.plurals.pollen_grains,
                                                    level.roundToInt(),
                                                    level.roundToInt(),
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        met?.let { m ->
            item {
                PvotListGroup(title = stringResource(R.string.air_quality_met)) {
                    row(
                        R.string.air_met_index,
                        String.format(Locale.getDefault(), "%.1f", m.index),
                        Formats.metAqiLevel(m.index),
                    )
                    concentration(R.string.air_pm25_name, m.pm25)
                    concentration(R.string.air_pm10_name, m.pm10)
                    concentration(R.string.air_no2, m.nitrogenDioxide)
                    concentration(R.string.air_ozone, m.ozone)
                }
            }
            m.area?.let { item { Note(stringResource(R.string.air_met_area, it)) } }
        }
    }
}

private fun PvotListGroupScope.row(title: Int, value: String, supporting: Int) = item {
    PvotListRow(
        title = stringResource(title),
        supportingText = stringResource(supporting),
        trailing = { Text(value, style = MaterialTheme.typography.titleMedium) },
    )
}

private fun PvotListGroupScope.concentration(name: Int, value: Double?) {
    value ?: return
    item {
        PvotListRow(
            title = stringResource(name),
            trailing = { Text(stringResource(R.string.air_ugm3, value.roundToInt().toString())) },
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

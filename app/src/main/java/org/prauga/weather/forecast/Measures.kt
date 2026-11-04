// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.prauga.weather.R
import org.prauga.weather.core.model.TemperatureUnit
import org.prauga.weather.settings.LocalUnits

@Composable
internal fun imperial(): Boolean = LocalUnits.current.temperature == TemperatureUnit.FAHRENHEIT

@Composable
private fun unit(@StringRes metric: Int, @StringRes us: Int): String =
    stringResource(if (imperial()) us else metric)

@Composable internal fun rainUnit() = unit(R.string.unit_mm, R.string.unit_in)

@Composable internal fun rain(mm: Double) = Formats.precipitation(mm, imperial())

@Composable
internal fun rainText(mm: Double) = stringResource(R.string.value_unit, rain(mm), rainUnit())

@Composable internal fun snowUnit() = unit(R.string.unit_cm, R.string.unit_in)

@Composable
internal fun snowDepthText(metres: Double) =
    stringResource(R.string.value_unit, Formats.snowDepth(metres, imperial()), snowUnit())

@Composable internal fun snowfall(cm: Double) = Formats.snowfall(cm, imperial())

@Composable
internal fun pressureText(hPa: Double) =
    stringResource(
        R.string.value_unit,
        Formats.pressure(hPa, imperial()),
        unit(R.string.unit_hpa, R.string.unit_inhg),
    )

@Composable
internal fun wavesText(metres: Double) =
    stringResource(
        R.string.value_unit,
        Formats.waves(metres, imperial()),
        unit(R.string.unit_m, R.string.unit_ft),
    )

@Composable
internal fun visibilityText(metres: Double) =
    stringResource(
        R.string.value_unit,
        Formats.visibility(metres, imperial()),
        unit(R.string.unit_km, R.string.unit_mi),
    )

@Composable internal fun flowValue(m3s: Double) = Formats.flow(m3s, imperial())

@Composable internal fun flowUnit() = unit(R.string.unit_m3s, R.string.unit_ft3s)

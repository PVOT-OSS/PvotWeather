// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.compare

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.designsystem.components.PvotCard
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt
import org.prauga.weather.R
import org.prauga.weather.SubScreen
import org.prauga.weather.core.data.Coverage
import org.prauga.weather.core.model.EnsembleHour
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.HourPoint
import org.prauga.weather.core.model.Sourced
import org.prauga.weather.forecast.Formats
import org.prauga.weather.placeTitle
import org.prauga.weather.settings.LocalUnits
import org.prauga.weather.settings.symbol
import org.prauga.weather.theme.WeatherColorsDefaults

private val OpenMeteoColor = WeatherColorsDefaults.Colors.openMeteo
private val MetColor = WeatherColorsDefaults.Colors.metNorway

private const val HOURS = 48

@Composable
fun CompareScreen(viewModel: CompareViewModel, onBack: () -> Unit) {
    val forecast by viewModel.forecast.collectAsStateWithLifecycle()
    val ensemble by viewModel.ensemble.collectAsStateWithLifecycle()
    val current = forecast
    SubScreen(stringResource(R.string.compare), onBack, Arrangement.spacedBy(16.dp)) {
        if (current == null) {
            item { PvotEmptyContent(stringResource(R.string.compare_empty)) }
            return@SubScreen
        }
        item { Intro(current) }
        item { Charts(current, ensemble) }
        item { Days(current) }
        item { Notes(current) }
    }
}

@Composable
private fun Intro(forecast: Forecast) {
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.compare_intro, placeTitle(forecast.place)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(OpenMeteoColor, stringResource(R.string.about_open_meteo))
            Legend(MetColor, stringResource(R.string.about_met))
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(
            label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Charts(forecast: Forecast, ensemble: List<EnsembleHour>) {
    val units = LocalUnits.current
    val hours = forecast.hours.take(HOURS)
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val labels =
        hours.indices
            .filter { it % 12 == 0 }
            .map { Formats.hour(hours[it].time, forecast.place.zone, is24) }
    val temperatureUnit = stringResource(units.temperature.symbol)
    val windUnit = stringResource(units.wind.symbol)
    val toUnit = { c: Double -> units.temperature.fromCelsius(c) }
    val metTemperature =
        Band(
            MetColor.copy(alpha = 0.18f),
            hours.map { it.temperatureLow?.let(toUnit) },
            hours.map { it.temperatureHigh?.let(toUnit) },
        )
    val matched =
        hours.map { h ->
            ensemble
                .minByOrNull { abs(Duration.between(it.time, h.time).toMinutes()) }
                ?.takeIf { abs(Duration.between(it.time, h.time).toMinutes()) <= 31 }
        }
    val ensembleBand =
        Band(
            OpenMeteoColor.copy(alpha = 0.16f),
            matched.map { it?.low?.let(toUnit) },
            matched.map { it?.high?.let(toUnit) },
        )
    val metWind =
        Band(
            MetColor.copy(alpha = 0.18f),
            hours.map { it.windLow?.let(units.wind::fromMetresPerSecond) },
            hours.map { it.windHigh?.let(units.wind::fromMetresPerSecond) },
        )
    val temperatureNotes = buildList {
        if (metTemperature.low.any { it != null }) add(stringResource(R.string.compare_band_met))
        matched
            .firstNotNullOfOrNull { it }
            ?.let {
                add(pluralStringResource(R.plurals.compare_band_ensemble, it.members, it.members))
            }
    }
    Column {
        ChartCard(
            stringResource(R.string.compare_temperature),
            hours,
            { it.temperature },
            { units.temperature.fromCelsius(it) },
            labels,
            bands = listOf(ensembleBand, metTemperature),
            notes = temperatureNotes,
        ) {
            "${it.roundToInt()}$temperatureUnit"
        }
        ChartCard(
            stringResource(R.string.compare_rain),
            hours,
            { it.precipitationProbability },
            { it },
            labels,
        ) {
            "${it.roundToInt()}%"
        }
        ChartCard(
            stringResource(R.string.compare_wind),
            hours,
            { it.windSpeed },
            { units.wind.fromMetresPerSecond(it) },
            labels,
            bands = listOf(metWind),
            notes =
                if (metWind.low.any { it != null })
                    listOf(stringResource(R.string.compare_band_met))
                else emptyList(),
        ) {
            "${it.roundToInt()} $windUnit"
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    hours: List<HourPoint>,
    value: (HourPoint) -> Sourced?,
    convert: (Double) -> Double,
    labels: List<String>,
    bands: List<Band> = emptyList(),
    notes: List<String> = emptyList(),
    format: (Double) -> String,
) {
    val om = hours.map { h -> value(h)?.openMeteo?.let(convert) }
    val met = hours.map { h -> value(h)?.metNorway?.let(convert) }
    if (om.all { it == null } && met.all { it == null }) return
    PvotCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            ComparisonChart(
                listOf(Series(OpenMeteoColor, om), Series(MetColor, met)),
                labels,
                format,
                bands = bands,
            )
            notes.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (met.all { it == null }) {
                Text(
                    stringResource(R.string.compare_met_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Days(forecast: Forecast) {
    val unit = LocalUnits.current.temperature
    fun Double?.text() = this?.let { "${Formats.degrees(it, unit)}°" } ?: "–"
    PvotCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.compare_days), style = MaterialTheme.typography.titleSmall)
            Row {
                Text("", Modifier.weight(1f))
                Header(stringResource(R.string.about_open_meteo), OpenMeteoColor)
                Header(stringResource(R.string.about_met), MetColor)
            }
            forecast.days
                .filter { it.temperatureMax != null || it.temperatureMin != null }
                .forEach { day ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Formats.weekday(day.date) + " " + day.date.dayOfMonth,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Cell(
                            "${day.temperatureMax?.openMeteo.text()} / ${day.temperatureMin?.openMeteo.text()}"
                        )
                        Cell(
                            "${day.temperatureMax?.metNorway.text()} / ${day.temperatureMin?.metNorway.text()}"
                        )
                    }
                }
        }
    }
}

@Composable
private fun Header(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier.width(96.dp),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun Cell(text: String) {
    Text(
        text,
        modifier = Modifier.width(96.dp),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun Notes(forecast: Forecast) {
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOfNotNull(
                stringResource(R.string.compare_horizon),
                forecast.openMeteoModel?.let { stringResource(R.string.compare_model, "ECMWF IFS") }
                    ?: stringResource(R.string.compare_model_default),
                if (Coverage.hasNowcast(forecast.place) && forecast.openMeteoModel == null) {
                    stringResource(R.string.compare_nordic)
                } else {
                    null
                },
            )
            .forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

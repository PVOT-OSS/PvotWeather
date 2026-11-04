// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.designsystem.components.PvotCard
import java.time.Instant
import java.time.ZoneId
import org.prauga.weather.R
import org.prauga.weather.core.model.DayPoint
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.HourPoint
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.model.Sourced

private val HighColor = Color(0xFFFF8A3D)
private val LowColor = Color(0xFF4FA3E0)
private val RainColor = Color(0xFF4FA3E0)

private const val TEMPERATURE_SPREAD = 3.0

@Composable
fun ForecastRoute(viewModel: ForecastViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // MET's terms: fetch only while the app is in use.
    LifecycleStartEffect(Unit) {
        viewModel.refresh()
        onStopOrDispose {}
    }
    ForecastScreen(state, onRefresh = { viewModel.refresh(force = true) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(state: ForecastUiState, onRefresh: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val forecast = state.forecast
    val current = forecast?.current
    Surface(
        Modifier.fillMaxSize(),
        color = background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize()) {
            val listState = rememberLazyListState()
            WeatherBackdrop(
                Backdrop.of(
                    current?.condition?.value,
                    current?.isDay,
                    current?.time ?: Instant.now(),
                    forecast?.days?.firstOrNull()?.sunrise,
                    forecast?.days?.firstOrNull()?.sunset,
                ),
                background,
                isSystemInDarkTheme(),
                Modifier.fillMaxWidth().fillMaxHeight(0.62f).graphicsLayer {
                    translationY =
                        if (listState.firstVisibleItemIndex == 0) {
                            -listState.firstVisibleItemScrollOffset.toFloat()
                        } else {
                            -size.height
                        }
                },
            )
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    forecast != null -> ForecastContent(state, forecast, listState)
                    state.loading || state.refreshing ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PvotLoadingContent()
                        }
                    else ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PvotErrorContent(
                                title = stringResource(R.string.no_forecast_title),
                                message = stringResource(R.string.no_forecast_message),
                                onRetry = onRefresh,
                            )
                        }
                }
            }
            StatusBarScrim(background)
        }
    }
}

@Composable
private fun StatusBarScrim(color: Color) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier.fillMaxWidth()
            .height(top + 12.dp)
            .background(Brush.verticalGradient(0.6f to color, 1f to color.copy(alpha = 0f)))
    )
}

@Composable
private fun ForecastContent(state: ForecastUiState, forecast: Forecast, listState: LazyListState) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = insets.calculateTopPadding() + 12.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    forecast.place.name,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(170.dp))
                NowBlock(forecast)
                HourlyStrip(forecast)
            }
        }
        item { DaysCard(forecast) }
        item { Tiles(forecast) }
        item { Footer(state) }
    }
}

@Composable
private fun NowBlock(forecast: Forecast) {
    val current = forecast.current ?: return
    val temperature = current.temperature ?: return
    val today = forecast.days.firstOrNull()
    var showSources by rememberSaveable { mutableStateOf(false) }
    val differs = (temperature.spread ?: 0.0) >= TEMPERATURE_SPREAD
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Row(
                Modifier.weight(1f).clickable(
                    onClickLabel = stringResource(R.string.sources_show)
                ) {
                    showSources = !showSources
                }
            ) {
                Text(
                    Formats.degrees(temperature.value).toString(),
                    style =
                        TextStyle(
                            fontSize = 104.sp,
                            lineHeight = 104.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-4).sp,
                        ),
                )
                Text(
                    stringResource(R.string.celsius_unit),
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp),
                    style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Light),
                )
                if (differs) DisagreementDot(Modifier.padding(top = 22.dp, start = 6.dp))
            }
            if (today != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 18.dp),
                ) {
                    today.temperatureMax?.let { HighLow("↑", it, R.string.high) }
                    today.temperatureMin?.let { HighLow("↓", it, R.string.low) }
                }
            }
        }
        Text(summary(forecast), style = MaterialTheme.typography.titleMedium)
        AnimatedVisibility(showSources) {
            Text(
                sourcesLine(temperature),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HighLow(arrow: String, value: Sourced, description: Int) {
    val text = stringResource(R.string.degrees_celsius, Formats.degrees(value.value))
    val spoken = stringResource(description, text)
    Text(
        "$arrow $text",
        modifier = Modifier.semantics { contentDescription = spoken },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun sourcesLine(value: Sourced): String {
    fun Double?.text() = this?.let { "%.1f°".format(it) } ?: "–"
    return stringResource(R.string.sources_values, value.openMeteo.text(), value.metNorway.text())
}

@Composable
private fun summary(forecast: Forecast): String {
    val current = forecast.current
    val condition = current?.condition?.value
    val next = forecast.hours.getOrNull(1)
    val probability = next?.precipitationProbability?.value ?: 0.0
    val nextWet =
        next?.condition?.value?.isWet == true || (next?.precipitation?.value ?: 0.0) >= 0.2
    return when {
        condition?.isWet == true || condition == null ->
            condition?.let { stringResource(Formats.condition(it, current.isDay)) } ?: ""
        probability >= 60 || (nextWet && probability >= 30) ->
            stringResource(R.string.rain_likely_next_hour)
        probability >= 30 || nextWet -> stringResource(R.string.rain_possible_next_hour)
        else -> stringResource(Formats.condition(condition, current.isDay))
    }
}

@Composable
private fun DisagreementDot(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.sources_differ)
    Box(
        modifier.size(6.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape).semantics {
            contentDescription = description
        }
    )
}

@Composable
private fun HourlyStrip(forecast: Forecast) {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val zone = forecast.place.zone
    val hours = forecast.hours.take(25)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(hours) { index, hour ->
            val shown = if (index == 0) forecast.current ?: hour else hour
            HourColumn(
                label =
                    if (index == 0) stringResource(R.string.now)
                    else Formats.hour(hour.time, zone, is24),
                hour = shown,
                highlighted = index == 0,
            )
        }
    }
}

@Composable
private fun HourColumn(label: String, hour: HourPoint, highlighted: Boolean) {
    val probability = hour.precipitationProbability?.value?.toInt() ?: 0
    val differs =
        hour.condition?.conflicts == true || (hour.temperature?.spread ?: 0.0) >= TEMPERATURE_SPREAD
    Column(
        Modifier.width(56.dp).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
        )
        ConditionIcon(hour.condition?.value, hour.isDay, Modifier.size(30.dp))
        Text(
            if (probability >= 20) stringResource(R.string.percent, probability) else "",
            style = MaterialTheme.typography.labelSmall,
            color = RainColor,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                hour.temperature?.let {
                    stringResource(R.string.degrees, Formats.degrees(it.value))
                } ?: "–",
                style = MaterialTheme.typography.titleSmall,
            )
            if (differs) DisagreementDot(Modifier.padding(start = 2.dp))
        }
    }
}

@Composable
private fun DaysCard(forecast: Forecast) {
    if (forecast.days.isEmpty()) return
    var showAll by rememberSaveable { mutableStateOf(false) }
    val days = if (showAll) forecast.days else forecast.days.take(7)
    val firstSingleSource = forecast.days.firstOrNull { it.temperatureMax?.metNorway == null }
    PvotCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.days_title, forecast.days.size),
                style = MaterialTheme.typography.titleSmall,
            )
            days.forEachIndexed { index, day -> DayRow(index, day, forecast) }
            if (forecast.days.size > 7) {
                TextButton(
                    onClick = { showAll = !showAll },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        if (showAll) stringResource(R.string.days_show_fewer)
                        else
                            pluralStringResource(
                                R.plurals.days_show_all,
                                forecast.days.size,
                                forecast.days.size,
                            )
                    )
                }
            }
            if (showAll && firstSingleSource != null && Provider.MET_NORWAY in forecast.providers) {
                Text(
                    stringResource(
                        R.string.days_single_source,
                        Formats.weekday(firstSingleSource.date),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayRow(index: Int, day: DayPoint, forecast: Forecast) {
    val label =
        when (index) {
            0 -> stringResource(R.string.today)
            1 -> stringResource(R.string.tomorrow)
            else -> Formats.weekday(day.date)
        }
    val probability = day.precipitationProbability?.value?.toInt() ?: 0
    val temperatures =
        forecast.hours
            .filter { it.time.atZone(forecast.place.zone).toLocalDate() == day.date }
            .mapNotNull { it.temperature?.value }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(84.dp), style = MaterialTheme.typography.bodyLarge)
        ConditionIcon(day.condition?.value, true, Modifier.size(24.dp))
        Text(
            if (probability >= 10) stringResource(R.string.percent, probability) else "",
            modifier = Modifier.width(48.dp).padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = RainColor,
        )
        Text(
            day.temperatureMax?.let {
                "↑" + stringResource(R.string.degrees, Formats.degrees(it.value))
            } ?: "",
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = HighColor,
        )
        Sparkline(temperatures, Modifier.weight(1f).height(22.dp).padding(horizontal = 8.dp))
        Text(
            day.temperatureMin?.let {
                "↓" + stringResource(R.string.degrees, Formats.degrees(it.value))
            } ?: "",
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = LowColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val range = (values.max() - min).coerceAtLeast(1.0)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (1 - ((v - min) / range)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            Brush.horizontalGradient(listOf(HighColor, LowColor)),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun Tiles(forecast: Forecast) {
    val current = forecast.current ?: return
    val today = forecast.days.firstOrNull()
    val zone = forecast.place.zone
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val tiles =
        buildList<@Composable (Modifier) -> Unit> {
            val apparent = current.apparentTemperature
            val actual = current.temperature
            if (apparent != null && actual != null) {
                add { m ->
                    Tile(
                        stringResource(R.string.feels_like),
                        stringResource(R.string.degrees, Formats.degrees(apparent.value)),
                        stringResource(Formats.feelsLike(actual.value, apparent.value)),
                        m,
                    )
                }
            }
            current.uvIndex?.let { uv ->
                val peak = today?.uvIndexMax?.value ?: uv.value
                add { m ->
                    Tile(
                        stringResource(R.string.uv_index),
                        uv.value.toInt().toString(),
                        if (peak < 3) stringResource(R.string.uv_low_all_day)
                        else
                            stringResource(
                                R.string.uv_peak,
                                stringResource(Formats.uvLevel(peak)).lowercase(),
                            ),
                        m,
                    ) {
                        Text(
                            stringResource(Formats.uvLevel(uv.value)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        UvBar(uv.value, Modifier.fillMaxWidth().height(6.dp))
                    }
                }
            }
            current.visibility?.let { v ->
                add { m ->
                    Tile(
                        stringResource(R.string.visibility),
                        stringResource(R.string.visibility_km, Formats.km(v.value)),
                        stringResource(Formats.visibility(v.value)),
                        m,
                        source =
                            if (v.metNorway == null) stringResource(R.string.open_meteo_only)
                            else null,
                    )
                }
            }
            current.humidity?.let { h ->
                add { m ->
                    Tile(
                        stringResource(R.string.humidity),
                        stringResource(R.string.percent, h.value.toInt()),
                        current.dewPoint?.let {
                            stringResource(
                                R.string.dew_point,
                                stringResource(R.string.degrees, Formats.degrees(it.value)),
                            )
                        } ?: "",
                        m,
                    )
                }
            }
            current.windSpeed?.let { w ->
                add { m ->
                    val from =
                        current.windDirection?.let { stringResource(Formats.compass(it.value)) }
                    val gust = current.windGust?.value?.let(Formats::kmh)
                    Tile(
                        stringResource(R.string.wind),
                        stringResource(R.string.wind_speed, Formats.kmh(w.value)),
                        when {
                            from != null && gust != null ->
                                stringResource(R.string.wind_from_gusts, from, gust)
                            from != null -> stringResource(R.string.wind_from, from)
                            else -> ""
                        },
                        m,
                    )
                }
            }
            val sunrise = today?.sunrise
            val sunset = today?.sunset
            if (sunrise != null && sunset != null) {
                add { m ->
                    Tile(
                        stringResource(R.string.sunrise),
                        Formats.time(sunrise, zone, is24),
                        stringResource(R.string.sunset, Formats.time(sunset, zone, is24)),
                        m,
                    )
                }
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Tile(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier,
    source: String? = null,
    extra: @Composable () -> Unit = {},
) {
    PvotCard(modifier) {
        Column(
            Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.headlineMedium)
            extra()
            Spacer(Modifier.weight(1f))
            Text(caption, style = MaterialTheme.typography.bodySmall)
            source?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UvBar(index: Double, modifier: Modifier) {
    val marker = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val h = size.height
        drawLine(
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF3DBE5C),
                    Color(0xFFF2D43D),
                    Color(0xFFF28A2E),
                    Color(0xFFE5483B),
                    Color(0xFF9B4FD1),
                )
            ),
            Offset(h / 2, h / 2),
            Offset(size.width - h / 2, h / 2),
            strokeWidth = h,
            cap = StrokeCap.Round,
        )
        val x = h / 2 + (size.width - h) * (index / 11.0).coerceIn(0.0, 1.0).toFloat()
        drawCircle(marker, h * 0.9f, Offset(x, h / 2))
    }
}

@Composable
private fun Footer(state: ForecastUiState) {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val zone = ZoneId.systemDefault()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            Provider.MET_NORWAY in state.failed && Provider.OPEN_METEO !in state.failed ->
                Text(
                    stringResource(R.string.met_failed),
                    style = MaterialTheme.typography.bodySmall,
                )
            Provider.OPEN_METEO in state.failed && Provider.MET_NORWAY !in state.failed ->
                Text(
                    stringResource(R.string.open_meteo_failed),
                    style = MaterialTheme.typography.bodySmall,
                )
        }
        state.fetchedAt.values.maxOrNull()?.let {
            Text(
                stringResource(R.string.updated_at, Formats.time(it, zone, is24)),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        Attribution(muted)
    }
}

@Composable
private fun Attribution(color: Color) {
    val template = stringResource(R.string.attribution)
    val links =
        listOf(
            stringResource(R.string.attribution_open_meteo) to "https://open-meteo.com/",
            stringResource(R.string.attribution_met) to "https://api.met.no/",
            stringResource(R.string.attribution_licence) to
                "https://creativecommons.org/licenses/by/4.0/",
        )
    val linkStyle = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline))
    val text = buildAnnotatedString {
        var last = 0
        Regex("%(\\d)\\\$s").findAll(template).forEach { match ->
            append(template.substring(last, match.range.first))
            val (label, url) = links[match.groupValues[1].toInt() - 1]
            withLink(LinkAnnotation.Url(url, linkStyle)) { append(label) }
            last = match.range.last + 1
        }
        append(template.substring(last))
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

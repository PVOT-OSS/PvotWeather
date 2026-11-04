// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import android.text.format.DateFormat
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import org.prauga.weather.R
import org.prauga.weather.core.data.Extras
import org.prauga.weather.core.model.AirQuality
import org.prauga.weather.core.model.Alert
import org.prauga.weather.core.model.AlertLevel
import org.prauga.weather.core.model.DayPoint
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.HourPoint
import org.prauga.weather.core.model.Place
import org.prauga.weather.core.model.Provider
import org.prauga.weather.core.model.Sourced
import org.prauga.weather.placeTitle
import org.prauga.weather.settings.LocalUnits
import org.prauga.weather.settings.symbol
import org.prauga.weather.theme.WeatherColorsDefaults

private val HighColor = WeatherColorsDefaults.Colors.high
private val LowColor = WeatherColorsDefaults.Colors.low
private val RainColor = WeatherColorsDefaults.Colors.rain

private const val TEMPERATURE_SPREAD = 3.0

data class MenuItem(@StringRes val label: Int, val onClick: () -> Unit)

@Composable
fun ForecastRoute(
    viewModel: ForecastViewModel,
    onOpenPlaces: (() -> Unit)?,
    menu: List<MenuItem> = emptyList(),
    onOpenAirQuality: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // MET's terms: fetch only while the app is in use.
    LifecycleStartEffect(Unit) {
        viewModel.refresh()
        onStopOrDispose {}
    }
    LaunchedEffect(state.place?.id) { if (state.place != null) viewModel.refresh() }
    ForecastScreen(
        state,
        onRefresh = { viewModel.refresh(force = true) },
        onOpenPlaces = onOpenPlaces,
        menu = listOfNotNull(onOpenPlaces?.let { MenuItem(R.string.places, it) }) + menu,
        onOpenAirQuality = onOpenAirQuality,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    state: ForecastUiState,
    onRefresh: () -> Unit,
    onOpenPlaces: (() -> Unit)?,
    menu: List<MenuItem>,
    onOpenAirQuality: () -> Unit = {},
) {
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
            if (forecast != null) {
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
            }
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val header: @Composable () -> Unit = {
                    PlaceHeader(state.place, onOpenPlaces, menu)
                }
                when {
                    forecast != null ->
                        ForecastContent(state, forecast, listState, header, onOpenAirQuality)
                    state.noPlaces ->
                        Status(header) {
                            PvotEmptyContent(
                                message = stringResource(R.string.no_places),
                                actionLabel =
                                    onOpenPlaces?.let { stringResource(R.string.add_place) },
                                onAction = onOpenPlaces,
                            )
                        }
                    state.loading || state.refreshing -> Status(header) { PvotLoadingContent() }
                    else ->
                        Status(header) {
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
internal fun degrees(celsius: Double): Int =
    Formats.degrees(celsius, LocalUnits.current.temperature)

@Composable
private fun wind(metresPerSecond: Double): Int =
    Formats.wind(metresPerSecond, LocalUnits.current.wind)

@Composable
private fun Status(header: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        Box(Modifier.padding(top = 12.dp)) { header() }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun PlaceHeader(place: Place?, onOpenPlaces: (() -> Unit)?, menu: List<MenuItem>) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f)
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (onOpenPlaces != null) {
                        Modifier.clickable(
                            onClickLabel = stringResource(R.string.places),
                            onClick = onOpenPlaces,
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(
                    if (place?.isCurrentLocation == true) R.drawable.ic_my_location
                    else R.drawable.ic_place
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                place?.let { placeTitle(it) } ?: stringResource(R.string.app_name),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            PvotIconButton(
                painterResource(R.drawable.ic_menu),
                stringResource(R.string.menu),
                { menuOpen = true },
                colors = PvotIconButtonDefaults.plainColors(),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                menu.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.label)) },
                        onClick = {
                            menuOpen = false
                            item.onClick()
                        },
                    )
                }
            }
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
private fun ForecastContent(
    state: ForecastUiState,
    forecast: Forecast,
    listState: LazyListState,
    header: @Composable () -> Unit,
    onOpenAirQuality: () -> Unit,
) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    PvotScreen(
        horizontalAlignment = Alignment.Start,
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
                header()
                Spacer(Modifier.height(150.dp))
                NowBlock(forecast, state.extras)
                HourlyStrip(forecast)
            }
        }
        if (state.extras.alerts.isNotEmpty())
            item { AlertsCard(state.extras.alerts, forecast.place.zone) }
        item { DaysCard(forecast) }
        item { Tiles(forecast, state.extras, onOpenAirQuality) }
        item { Footer(state) }
    }
}

@Composable
private fun NowBlock(forecast: Forecast, extras: Extras) {
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
                    degrees(temperature.value).toString(),
                    style =
                        TextStyle(
                            fontSize = 104.sp,
                            lineHeight = 104.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-4).sp,
                        ),
                )
                Text(
                    stringResource(LocalUnits.current.temperature.symbol),
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
        Text(summary(forecast, extras), style = MaterialTheme.typography.titleMedium)
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
    val text =
        stringResource(
            R.string.degrees_unit,
            degrees(value.value),
            stringResource(LocalUnits.current.temperature.symbol),
        )
    val spoken = stringResource(description, text)
    Text(
        "$arrow $text",
        modifier = Modifier.semantics { contentDescription = spoken },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun sourcesLine(value: Sourced): String {
    val unit = LocalUnits.current.temperature
    fun Double?.text() = this?.let { "%.1f°".format(unit.fromCelsius(it)) } ?: "–"
    return stringResource(R.string.sources_values, value.openMeteo.text(), value.metNorway.text())
}

@Composable
private fun summary(forecast: Forecast, extras: Extras): String {
    val steps = extras.nowcast?.steps ?: forecast.minutely
    when (val outlook = RainOutlook.of(steps, Instant.now())) {
        is RainOutlook.Starting ->
            if (outlook.minutes <= 120)
                return stringResource(R.string.rain_starting, outlook.minutes)
        is RainOutlook.Stopping -> return stringResource(R.string.rain_stopping, outlook.minutes)
        RainOutlook.Continuing -> return stringResource(R.string.rain_continuing)
        RainOutlook.Dry,
        null -> Unit
    }
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
    val showChance = hours.any { (it.precipitationProbability?.value ?: 0.0) >= 20 }
    val showAmount = hours.any { (it.precipitation?.value ?: 0.0) >= 0.1 }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(hours) { index, hour ->
            val shown = if (index == 0) forecast.current ?: hour else hour
            HourColumn(
                label =
                    if (index == 0) stringResource(R.string.now)
                    else Formats.hour(hour.time, zone, is24),
                hour = shown,
                highlighted = index == 0,
                showChance = showChance,
                showAmount = showAmount,
            )
        }
    }
}

@Composable
private fun HourColumn(
    label: String,
    hour: HourPoint,
    highlighted: Boolean,
    showChance: Boolean,
    showAmount: Boolean,
) {
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
        if (showChance) {
            Text(
                if (probability >= 20) stringResource(R.string.percent, probability) else "",
                style = MaterialTheme.typography.labelSmall,
                color = RainColor,
            )
        }
        if (showAmount) {
            Text(
                hour.precipitation
                    ?.value
                    ?.takeIf { it >= 0.1 }
                    ?.let { stringResource(R.string.mm, Formats.millimetres(it)) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = RainColor,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                hour.temperature?.let { stringResource(R.string.degrees, degrees(it.value)) }
                    ?: "–",
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
            forecast.yesterday?.let { Box(Modifier.alpha(0.6f)) { DayRow(-1, it, forecast) } }
            days.forEachIndexed { index, day -> DayRow(index, day, forecast) }
            if (forecast.days.size > 7) {
                PvotButton(
                    text =
                        if (showAll) stringResource(R.string.days_show_fewer)
                        else
                            pluralStringResource(
                                R.plurals.days_show_all,
                                forecast.days.size,
                                forecast.days.size,
                            ),
                    onClick = { showAll = !showAll },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = PvotButtonStyle.Text,
                )
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
            -1 -> stringResource(R.string.yesterday)
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
            day.temperatureMax?.let { "↑" + stringResource(R.string.degrees, degrees(it.value)) }
                ?: "",
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = HighColor,
        )
        Sparkline(temperatures, Modifier.weight(1f).height(22.dp).padding(horizontal = 8.dp))
        Text(
            day.temperatureMin?.let { "↓" + stringResource(R.string.degrees, degrees(it.value)) }
                ?: "",
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
private fun Tiles(forecast: Forecast, extras: Extras, onOpenAirQuality: () -> Unit) {
    val usualHigh = extras.usualHigh
    val current = forecast.current ?: return
    val today = forecast.days.firstOrNull()
    val zone = forecast.place.zone
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val tiles =
        buildList<@Composable (Modifier) -> Unit> {
            val todayHigh = today?.temperatureMax?.value
            if (usualHigh != null && todayHigh != null) {
                add { m -> AverageTile(todayHigh, usualHigh, m) }
            }
            val apparent = current.apparentTemperature
            val actual = current.temperature
            if (apparent != null && actual != null) {
                add { m ->
                    Tile(
                        stringResource(R.string.feels_like),
                        stringResource(R.string.degrees, degrees(apparent.value)),
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
            extras.airQuality?.let { air -> add { m -> AirQualityTile(air, m, onOpenAirQuality) } }
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
                                stringResource(R.string.degrees, degrees(it.value)),
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
                    val gust = current.windGust?.value?.let { wind(it) }
                    Tile(
                        stringResource(R.string.wind),
                        stringResource(
                            R.string.wind_speed,
                            wind(w.value),
                            stringResource(LocalUnits.current.wind.symbol),
                        ),
                        when {
                            from != null && gust != null ->
                                stringResource(
                                    R.string.wind_from_gusts,
                                    from,
                                    gust,
                                    stringResource(LocalUnits.current.wind.symbol),
                                )
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
            addAll(moreTiles(forecast, extras))
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
private fun AverageTile(todayHigh: Double, usualHigh: Double, modifier: Modifier) {
    val unit = LocalUnits.current.temperature
    val difference = degrees(todayHigh) - degrees(usualHigh)
    val caption =
        when {
            todayHigh - usualHigh >= 1.5 -> R.string.average_above
            usualHigh - todayHigh >= 1.5 -> R.string.average_below
            else -> R.string.average_close
        }
    Tile(
        stringResource(R.string.average),
        (if (difference > 0) "+" else if (difference < 0) "−" else "") +
            stringResource(R.string.degrees, abs(difference)),
        stringResource(caption),
        modifier,
        source = stringResource(R.string.average_source),
    ) {
        Text(
            stringResource(
                R.string.average_detail,
                stringResource(R.string.degrees, degrees(todayHigh)),
                stringResource(R.string.degrees, degrees(usualHigh)),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AirQualityTile(air: AirQuality, modifier: Modifier, onOpen: () -> Unit) {
    Tile(
        stringResource(R.string.air_quality),
        air.usAqi.toString(),
        listOfNotNull(
                air.pm25?.let { stringResource(R.string.air_pm25, it.roundToInt()) },
                air.pm10?.let { stringResource(R.string.air_pm10, it.roundToInt()) },
                air.europeanAqi?.let { stringResource(R.string.air_european, it) },
            )
            .joinToString("\n"),
        modifier,
        source = stringResource(R.string.air_source),
        onClick = onOpen,
    ) {
        Text(
            stringResource(Formats.aqiLevel(air.usAqi)),
            style = MaterialTheme.typography.titleMedium,
        )
        AqiBar(air.usAqi, Modifier.fillMaxWidth().height(6.dp))
    }
}

@Composable
private fun AqiBar(aqi: Int, modifier: Modifier) {
    val marker = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val h = size.height
        drawLine(
            Brush.horizontalGradient(WeatherColorsDefaults.Colors.aqiScale),
            Offset(h / 2, h / 2),
            Offset(size.width - h / 2, h / 2),
            strokeWidth = h,
            cap = StrokeCap.Round,
        )
        val x = h / 2 + (size.width - h) * (aqi / 300f).coerceIn(0f, 1f)
        drawCircle(marker, h * 0.9f, Offset(x, h / 2))
    }
}

@Composable
private fun AlertsCard(alerts: List<Alert>, zone: ZoneId) {
    val uriHandler = LocalUriHandler.current
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts
            .sortedByDescending { it.level }
            .forEach { alert ->
                PvotCard(
                    Modifier.fillMaxWidth(),
                    onClick = alert.url?.let { url -> { uriHandler.openUri(url) } },
                ) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(
                            Modifier.width(6.dp).fillMaxHeight().background(alertColor(alert.level))
                        )
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(alertLevelLabel(alert.level)) + " · " + alert.event,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                listOfNotNull(
                                        alert.area.ifEmpty { null },
                                        alert.ends?.let {
                                            stringResource(
                                                R.string.alert_until,
                                                Formats.time(it, zone, is24),
                                            )
                                        },
                                    )
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(alert.description, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.alert_source),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
    }
}

private fun alertColor(level: AlertLevel) =
    when (level) {
        AlertLevel.YELLOW -> WeatherColorsDefaults.Colors.alertYellow
        AlertLevel.ORANGE -> WeatherColorsDefaults.Colors.alertOrange
        AlertLevel.RED -> WeatherColorsDefaults.Colors.alertRed
    }

private fun alertLevelLabel(level: AlertLevel) =
    when (level) {
        AlertLevel.YELLOW -> R.string.alert_level_yellow
        AlertLevel.ORANGE -> R.string.alert_level_orange
        AlertLevel.RED -> R.string.alert_level_red
    }

@Composable
internal fun Tile(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier,
    source: String? = null,
    onClick: (() -> Unit)? = null,
    extra: @Composable () -> Unit = {},
) {
    PvotCard(modifier, onClick = onClick) {
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
            Brush.horizontalGradient(WeatherColorsDefaults.Colors.uvScale),
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

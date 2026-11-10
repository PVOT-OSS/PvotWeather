// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.CardColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotCardDefaults
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import java.time.Instant
import java.time.ZoneId
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

/** Cool to warm, in °C: the colours the day bars run through. */
private val TemperatureScale =
    listOf(
        -10.0 to Color(0xFF7AA7FF),
        5.0 to Color(0xFF6EC6E8),
        15.0 to Color(0xFFA8D98F),
        22.0 to Color(0xFFF5CD4B),
        30.0 to Color(0xFFF39A3B),
        38.0 to Color(0xFFE8533B),
    )

/** How far text inside a card sits from its edge; text straight on the sky starts there too. */
private val CardInset = 16.dp

/** Days in the day list. */
private const val DAYS_SHOWN = 10

/** Opacity of hairlines and tracks drawn in the sky's ink. */
private const val HAIRLINE_ALPHA = 0.15f

private val RainColor = WeatherColorsDefaults.Colors.rain

private const val TEMPERATURE_SPREAD = 3.0

/** Opacity of secondary text drawn straight on the sky, against the sky's own text colour. */
private const val ON_SKY_MUTED = 0.75f

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
    val dark = isSystemInDarkTheme()
    val forecast = state.forecast
    val current = forecast?.current
    val backdrop =
        forecast?.let {
            Backdrop.of(
                current?.condition?.value,
                current?.isDay,
                current?.time ?: Instant.now(),
                it.days.firstOrNull()?.sunrise,
                it.days.firstOrNull()?.sunset,
            )
        }
    Surface(
        Modifier.fillMaxSize(),
        color = background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize()) {
            val listState = rememberLazyListState()
            // The sky fills the screen and stays put while the forecast scrolls over it.
            if (backdrop != null) WeatherBackdrop(backdrop, dark, Modifier.fillMaxSize())
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
                        OnSky(backdrop?.onSky(background, dark)) {
                            ForecastContent(state, forecast, listState, header, onOpenAirQuality)
                        }
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
            StatusBarScrim(backdrop?.top(background, dark) ?: background)
            StatusBarIcons(lightBackground = backdrop?.isLight(background, dark))
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
                colors = PvotIconButtonDefaults.plainColors(LocalContentColor.current),
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

/** Card fill over a sky drawn in white ink: the sky, a shade darker. */
private val SkyCardUnderLightInk = Color.Black.copy(alpha = 0.2f)

/** Card fill over a sky drawn in dark ink: the sky, a shade lighter. */
private val SkyCardUnderDarkInk = Color.White.copy(alpha = 0.3f)

/** The fill [skyCardColors] gives cards, or null off the sky. */
private val LocalSkyCard = staticCompositionLocalOf<Color?> { null }

/**
 * Draws [content] straight on the sky in [ink]: text in [ink], secondary text in a muted [ink], and
 * cards as a translucent shade of the sky rather than the theme's surface. Null [ink] leaves the
 * theme as it is.
 */
@Composable
private fun OnSky(ink: Color?, content: @Composable () -> Unit) {
    if (ink == null) {
        content()
        return
    }
    MaterialTheme(
        colorScheme =
            MaterialTheme.colorScheme.copy(onSurfaceVariant = ink.copy(alpha = ON_SKY_MUTED)),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides ink,
            LocalSkyCard provides
                if (ink == Color.White) SkyCardUnderLightInk else SkyCardUnderDarkInk,
            content = content,
        )
    }
}

@Composable
private fun skyCardColors(): CardColors =
    LocalSkyCard.current?.let { PvotCardDefaults.colors(containerColor = it) }
        ?: PvotCardDefaults.colors()

/** Fades content scrolling under the status bar into [color], the colour behind it. */
@Composable
private fun StatusBarScrim(color: Color) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier.fillMaxWidth()
            .height(top + 12.dp)
            .background(Brush.verticalGradient(0.6f to color, 1f to color.copy(alpha = 0f)))
    )
}

/**
 * Colours the status-bar icons for [lightBackground]: dark icons over a light one, light icons over
 * a dark one, or the theme's own choice when null. Puts the theme's choice back on leaving.
 */
@Composable
private fun StatusBarIcons(lightBackground: Boolean?) {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }
    val themeDefault = remember(controller) { controller.isAppearanceLightStatusBars }
    DisposableEffect(controller, lightBackground) {
        controller.isAppearanceLightStatusBars = lightBackground ?: themeDefault
        onDispose { controller.isAppearanceLightStatusBars = themeDefault }
    }
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
                // Text straight on the sky starts on the line the text inside the cards does.
                Column(
                    Modifier.padding(horizontal = CardInset),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(Modifier.height(150.dp))
                    NowBlock(forecast, state.extras)
                }
                PvotCard(Modifier.fillMaxWidth(), colors = skyCardColors()) {
                    HourlyStrip(forecast)
                }
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
                val digits = degrees(temperature.value).toString()
                val digitsStyle =
                    MaterialTheme.typography.displayLarge.copy(
                        fontSize = 104.sp,
                        lineHeight = 104.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-4).sp,
                    )
                Text(digits, modifier = Modifier.inkStart(digits, digitsStyle), style = digitsStyle)
                Text(
                    stringResource(LocalUnits.current.temperature.symbol),
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp),
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = 40.sp,
                            lineHeight = TextUnit.Unspecified,
                            fontWeight = FontWeight.Light,
                            letterSpacing = TextUnit.Unspecified,
                        ),
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
                color = LocalContentColor.current.copy(alpha = ON_SKY_MUTED),
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
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
                hour.precipitation?.value?.takeIf { it >= 0.1 }?.let { rainText(it) } ?: "",
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
    val days = forecast.days.take(DAYS_SHOWN)
    if (days.isEmpty()) return
    val rows = listOfNotNull(forecast.yesterday) + days
    val temperatures =
        rows.flatMap { listOfNotNull(it.temperatureMin?.value, it.temperatureMax?.value) }
    val coolest = temperatures.minOrNull() ?: return
    val warmest = temperatures.maxOrNull() ?: return
    val divider = LocalContentColor.current.copy(alpha = HAIRLINE_ALPHA)
    PvotCard(Modifier.fillMaxWidth(), colors = skyCardColors()) {
        Column(Modifier.padding(horizontal = CardInset, vertical = 4.dp)) {
            forecast.yesterday?.let {
                Box(Modifier.alpha(0.6f)) { DayRow(-1, it, forecast, coolest, warmest) }
                HorizontalDivider(thickness = 0.5.dp, color = divider)
            }
            days.forEachIndexed { index, day ->
                if (index > 0) HorizontalDivider(thickness = 0.5.dp, color = divider)
                DayRow(index, day, forecast, coolest, warmest)
            }
        }
    }
}

/**
 * One day: its name, its condition with the chance of rain under it, and its low and high either
 * side of a bar placed on the [coolest]-to-[warmest] scale of every day shown. Today's bar marks
 * the temperature now.
 */
@Composable
private fun DayRow(
    index: Int,
    day: DayPoint,
    forecast: Forecast,
    coolest: Double,
    warmest: Double,
) {
    val label =
        when (index) {
            -1 -> stringResource(R.string.yesterday)
            0 -> stringResource(R.string.today)
            1 -> stringResource(R.string.tomorrow)
            else -> Formats.weekday(day.date)
        }
    val probability = day.precipitationProbability?.value?.toInt() ?: 0
    val low = day.temperatureMin?.value
    val high = day.temperatureMax?.value
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ConditionIcon(day.condition?.value, true, Modifier.size(24.dp))
            if (probability >= 10) {
                Text(
                    stringResource(R.string.percent, probability),
                    style = MaterialTheme.typography.labelSmall,
                    color = RainColor,
                )
            }
        }
        DayTemperature(low, R.string.low, MaterialTheme.colorScheme.onSurfaceVariant)
        TemperatureBar(
            low,
            high,
            now = if (index == 0) forecast.current?.temperature?.value else null,
            coolest = coolest,
            warmest = warmest,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp).height(6.dp),
        )
        DayTemperature(high, R.string.high, LocalContentColor.current)
    }
}

@Composable
private fun DayTemperature(celsius: Double?, @StringRes description: Int, color: Color) {
    val text = celsius?.let { stringResource(R.string.degrees, degrees(it)) } ?: ""
    val spoken = if (celsius != null) stringResource(description, text) else ""
    Text(
        text,
        modifier = Modifier.width(40.dp).semantics { contentDescription = spoken },
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        textAlign = TextAlign.End,
    )
}

/**
 * A day's [low] to [high] as a rounded bar on a track spanning [coolest] to [warmest], coloured
 * from cool to warm, with a dot at [now] when given.
 */
@Composable
private fun TemperatureBar(
    low: Double?,
    high: Double?,
    now: Double?,
    coolest: Double,
    warmest: Double,
    modifier: Modifier,
) {
    val ink = LocalContentColor.current
    val track = ink.copy(alpha = HAIRLINE_ALPHA)
    val ring = if (ink == Color.White) Color.Black.copy(alpha = 0.4f) else Color.White
    Canvas(modifier) {
        val radius = size.height / 2
        drawRoundRect(track, cornerRadius = CornerRadius(radius))
        if (low == null || high == null) return@Canvas
        val span = (warmest - coolest).coerceAtLeast(1.0)
        fun x(celsius: Double) = (size.width * ((celsius - coolest) / span)).toFloat()
        val start = x(low)
        val end = maxOf(x(high), start + size.height)
        drawRoundRect(
            Brush.horizontalGradient(
                0f to temperatureColor(low),
                0.5f to temperatureColor((low + high) / 2),
                1f to temperatureColor(high),
                startX = start,
                endX = end,
            ),
            topLeft = Offset(start, 0f),
            size = Size(end - start, size.height),
            cornerRadius = CornerRadius(radius),
        )
        if (now != null) {
            val centre = Offset(x(now.coerceIn(low, high)), radius)
            drawCircle(ring, radius = radius + 1.5.dp.toPx(), center = centre)
            drawCircle(ink, radius = radius, center = centre)
        }
    }
}

/** The colour of [celsius] on the cool-to-warm scale the day bars use. */
private fun temperatureColor(celsius: Double): Color {
    val scale = TemperatureScale
    if (celsius <= scale.first().first) return scale.first().second
    if (celsius >= scale.last().first) return scale.last().second
    val upper = scale.indexOfFirst { it.first >= celsius }
    val (t0, c0) = scale[upper - 1]
    val (t1, c1) = scale[upper]
    return lerp(c0, c1, ((celsius - t0) / (t1 - t0)).toFloat())
}

@Composable
private fun Tiles(forecast: Forecast, extras: Extras, onOpenAirQuality: () -> Unit) {
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
                    val feels = Formats.feelsLike(actual.value, apparent.value)
                    Tile(
                        stringResource(R.string.feels_like),
                        stringResource(R.string.degrees, degrees(apparent.value)),
                        if (feels == R.string.feels_same) "" else stringResource(feels),
                        m,
                        icon = R.drawable.ic_w_temperature,
                    )
                }
            }
            current.uvIndex?.let { uv ->
                add { m ->
                    Tile(
                        stringResource(R.string.uv_index),
                        uv.value.toInt().toString(),
                        "",
                        m,
                        icon = R.drawable.ic_w_uv,
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
            current.windSpeed?.let { w ->
                add { m ->
                    val unit = stringResource(LocalUnits.current.wind.symbol)
                    Tile(
                        stringResource(R.string.wind),
                        stringResource(R.string.wind_speed, wind(w.value), unit),
                        current.windGust?.value?.let {
                            stringResource(R.string.wind_gusts, wind(it), unit)
                        }
                            ?: current.windDirection?.let {
                                stringResource(
                                    R.string.wind_from,
                                    stringResource(Formats.compass(it.value)),
                                )
                            }
                            ?: "",
                        m,
                        icon = R.drawable.ic_w_wind,
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
                        icon = R.drawable.ic_w_droplet,
                    )
                }
            }
            current.visibility?.let { v ->
                add { m ->
                    Tile(
                        stringResource(R.string.visibility),
                        visibilityText(v.value),
                        "",
                        m,
                        icon = R.drawable.ic_w_eye,
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
                        icon = R.drawable.ic_w_sunrise,
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

/**
 * Moves [text] drawn in [style] left by its first glyph's side bearing, so the ink of large text
 * starts on the same line as the smaller text around it rather than a few dp in.
 */
@Composable
private fun Modifier.inkStart(text: String, style: TextStyle): Modifier {
    val resolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val typeface =
        remember(resolver, style) {
            resolver.resolve(style.fontFamily, style.fontWeight ?: FontWeight.Normal).value
                as? Typeface
        } ?: return this
    val bearing =
        remember(text, typeface, style, density) {
            val paint =
                Paint().apply {
                    this.typeface = typeface
                    textSize = with(density) { style.fontSize.toPx() }
                    if (style.letterSpacing.isSp) {
                        letterSpacing = style.letterSpacing.value / style.fontSize.value
                    }
                }
            Rect().also { paint.getTextBounds(text, 0, text.length, it) }.left
        }
    return offset { IntOffset(-bearing, 0) }
}

@Composable
private fun AirQualityTile(air: AirQuality, modifier: Modifier, onOpen: () -> Unit) {
    Tile(
        stringResource(R.string.air_quality),
        air.usAqi.toString(),
        "",
        modifier,
        icon = R.drawable.ic_w_leaf,
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
                    colors = skyCardColors(),
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

/**
 * A tile in the grid under the day list: [icon] and [title] on top, the [value] large, [extra]
 * under it, and a one-line [caption], if any, at the foot of the tile.
 */
@Composable
internal fun Tile(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null,
    extra: @Composable () -> Unit = {},
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    PvotCard(modifier, onClick = onClick, colors = skyCardColors()) {
        Column(
            Modifier.padding(CardInset).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                icon?.let {
                    Icon(
                        painterResource(it),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = muted,
                    )
                }
                Text(title, style = MaterialTheme.typography.labelLarge, color = muted)
            }
            Text(value, style = MaterialTheme.typography.headlineMedium)
            extra()
            Spacer(Modifier.weight(1f))
            if (caption.isNotBlank()) Text(caption, style = MaterialTheme.typography.bodyMedium)
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
    val muted = LocalContentColor.current.copy(alpha = ON_SKY_MUTED)
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

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.weather.R

@Composable
fun BackdropPreviewScreen(onBack: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val entries = Backdrop.entries
    val backdrop = entries[index]
    val background = MaterialTheme.colorScheme.background
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    PvotScreenScaffold(
        background = { WeatherBackdrop(backdrop, isSystemInDarkTheme(), Modifier.fillMaxSize()) },
        topBar = { PvotTopBar(stringResource(R.string.backdrops), onBack = onBack, compact = true) },
    ) { padding ->
        CompositionLocalProvider(
            LocalContentColor provides backdrop.onSky(background, isSystemInDarkTheme())
        ) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(bottom = bottom + PvotSpacing.L),
                verticalArrangement = Arrangement.spacedBy(PvotSpacing.L, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(backdrop.label()),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(R.string.backdrop_position, index + 1, entries.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M)) {
                    PvotButton(
                        stringResource(R.string.previous),
                        { index = (index + entries.size - 1) % entries.size },
                        style = PvotButtonStyle.Tonal,
                    )
                    PvotButton(
                        stringResource(R.string.next),
                        { index = (index + 1) % entries.size },
                        style = PvotButtonStyle.Tonal,
                    )
                }
            }
        }
    }
}

private fun Backdrop.label(): Int =
    when (this) {
        Backdrop.CLEAR -> R.string.backdrop_clear
        Backdrop.PARTLY_CLOUDY -> R.string.backdrop_partly_cloudy
        Backdrop.CLOUDY -> R.string.backdrop_cloudy
        Backdrop.LIGHT_RAIN -> R.string.backdrop_light_rain
        Backdrop.HEAVY_RAIN -> R.string.backdrop_heavy_rain
        Backdrop.THUNDERSTORM -> R.string.backdrop_thunderstorm
        Backdrop.SNOW -> R.string.backdrop_snow
        Backdrop.FOG -> R.string.backdrop_fog
        Backdrop.SUNRISE_SUNSET -> R.string.backdrop_sunrise_sunset
        Backdrop.NIGHT -> R.string.backdrop_night
    }

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt
import org.prauga.weather.R
import org.prauga.weather.core.data.Extras
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.HourPoint

private typealias TileContent = @Composable (Modifier) -> Unit

/**
 * The tiles after the always-on ones: precipitation, then thunder, fog, snow and pollen, each only
 * when there is some.
 */
internal fun moreTiles(forecast: Forecast, extras: Extras): List<TileContent> = buildList {
    val current = forecast.current ?: return@buildList
    val next24 = forecast.hours.take(24)

    precipitationTile(next24)?.let(::add)

    forecast.hours
        .take(12)
        .mapNotNull { it.thunderProbability?.value }
        .maxOrNull()
        ?.takeIf { it >= THUNDER_LIKELY }
        ?.let { thunder ->
            add { m ->
                Tile(
                    stringResource(R.string.thunder),
                    stringResource(R.string.percent, thunder.roundToInt()),
                    stringResource(R.string.thunder_possible),
                    m,
                    icon = R.drawable.ic_w_storm,
                )
            }
        }

    current.fog
        ?.value
        ?.takeIf { it >= FOG_PRESENT }
        ?.let { fog ->
            add { m ->
                Tile(
                    stringResource(R.string.fog),
                    stringResource(R.string.percent, fog.roundToInt()),
                    stringResource(R.string.fog_present),
                    m,
                    icon = R.drawable.ic_w_fog,
                )
            }
        }

    val depth = current.snowDepth?.value ?: 0.0
    val falling = next24.sumOf { it.snowfall?.value ?: 0.0 }
    if (depth > 0 || falling > 0) {
        add { m ->
            Tile(
                stringResource(R.string.snow),
                snowDepthText(depth),
                stringResource(R.string.snow_falling, snowfall(falling), snowUnit()),
                m,
                icon = R.drawable.ic_w_snow,
            )
        }
    }

    extras.airQuality
        ?.pollen
        ?.takeIf { it.isNotEmpty() }
        ?.maxBy { it.value }
        ?.takeIf { it.value >= 1 }
        ?.let { (worst, level) ->
            add { m ->
                Tile(
                    stringResource(R.string.pollen),
                    stringResource(Formats.pollenName(worst)),
                    pluralStringResource(
                        R.plurals.pollen_grains,
                        level.roundToInt(),
                        level.roundToInt(),
                    ),
                    m,
                    icon = R.drawable.ic_w_leaf,
                )
            }
        }
}

/** Thunder chance, in percent, from which the thunder tile shows. */
private const val THUNDER_LIKELY = 10.0

/** Fog cover, in percent, from which the fog tile shows. */
private const val FOG_PRESENT = 20.0

private fun precipitationTile(next24: List<HourPoint>): TileContent? {
    val amounts = next24.mapNotNull { it.precipitation?.value }
    if (amounts.isEmpty()) return null
    val total = amounts.sum()
    return { m ->
        Tile(
            stringResource(R.string.precipitation),
            rainText(total),
            stringResource(R.string.precipitation_next_24),
            m,
            icon = R.drawable.ic_w_umbrella,
        )
    }
}

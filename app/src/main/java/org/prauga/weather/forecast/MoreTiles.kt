// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.forecast

import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt
import org.prauga.weather.R
import org.prauga.weather.core.data.Extras
import org.prauga.weather.core.model.Forecast
import org.prauga.weather.core.model.HourPoint

private typealias TileContent = @Composable (Modifier) -> Unit

internal fun moreTiles(forecast: Forecast, extras: Extras): List<TileContent> = buildList {
    val current = forecast.current ?: return@buildList
    val next24 = forecast.hours.take(24)
    val today = forecast.days.firstOrNull()

    precipitationTile(next24, today?.precipitationSum?.value)?.let(::add)

    current.pressure?.let { pressure ->
        val later = forecast.hours.getOrNull(3)?.pressure?.value
        add { m ->
            Tile(
                stringResource(R.string.pressure),
                stringResource(R.string.pressure_hpa, pressure.value.roundToInt()),
                stringResource(Formats.pressureTrend(pressure.value, later)),
                m,
            )
        }
    }

    current.cloudCover?.let { cover ->
        add { m ->
            Tile(
                stringResource(R.string.cloud_cover),
                stringResource(R.string.percent, cover.value.roundToInt()),
                listOfNotNull(
                        current.cloudLow?.let {
                            stringResource(R.string.cloud_low, it.value.roundToInt())
                        },
                        current.cloudMid?.let {
                            stringResource(R.string.cloud_mid, it.value.roundToInt())
                        },
                        current.cloudHigh?.let {
                            stringResource(R.string.cloud_high, it.value.roundToInt())
                        },
                    )
                    .joinToString("\n"),
                m,
            )
        }
    }

    forecast.hours
        .take(12)
        .mapNotNull { it.thunderProbability?.value }
        .maxOrNull()
        ?.let { thunder ->
            add { m ->
                Tile(
                    stringResource(R.string.thunder),
                    stringResource(R.string.percent, thunder.roundToInt()),
                    stringResource(
                        if (thunder >= 10) R.string.thunder_possible else R.string.thunder_unlikely
                    ),
                    m,
                    source = stringResource(R.string.met_only),
                )
            }
        }

    current.fog?.value?.let { fog ->
        add { m ->
            Tile(
                stringResource(R.string.fog),
                stringResource(R.string.percent, fog.roundToInt()),
                stringResource(if (fog >= 20) R.string.fog_present else R.string.fog_none),
                m,
                source = stringResource(R.string.met_only),
            )
        }
    }

    val depth = current.snowDepth?.value ?: 0.0
    val falling = next24.sumOf { it.snowfall?.value ?: 0.0 }
    if (depth > 0 || falling > 0) {
        add { m ->
            Tile(
                stringResource(R.string.snow),
                stringResource(R.string.snow_depth_cm, (depth * 100).roundToInt()),
                stringResource(
                    R.string.snow_falling,
                    String.format(java.util.Locale.getDefault(), "%.1f", falling),
                ),
                m,
                source = stringResource(R.string.open_meteo_only),
            )
        }
    }

    val sunshine = today?.sunshineSeconds
    val daylight = today?.daylightSeconds
    if (sunshine != null && daylight != null) {
        add { m ->
            Tile(
                stringResource(R.string.sunshine),
                stringResource(R.string.hours_short, Formats.hours(sunshine)),
                stringResource(R.string.sunshine_of_daylight, Formats.hours(daylight)),
                m,
                source = stringResource(R.string.open_meteo_only),
            )
        }
    }

    extras.moon?.let { moon ->
        add { m ->
            val is24 = DateFormat.is24HourFormat(LocalContext.current)
            val zone = forecast.place.zone
            Tile(
                stringResource(R.string.moon),
                stringResource(Formats.moonPhase(moon.phaseDegrees)),
                listOfNotNull(
                        moon.rise?.let {
                            stringResource(R.string.moonrise, Formats.time(it, zone, is24))
                        },
                        moon.set?.let {
                            stringResource(R.string.moonset, Formats.time(it, zone, is24))
                        },
                    )
                    .joinToString("\n"),
                m,
                source = stringResource(R.string.met_only),
            ) {
                Text(
                    stringResource(R.string.moon_lit, Formats.moonIllumination(moon.phaseDegrees)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    extras.sea?.let { sea ->
        add { m ->
            Tile(
                stringResource(R.string.sea),
                sea.waveHeight?.let {
                    stringResource(
                        R.string.waves_m,
                        String.format(java.util.Locale.getDefault(), "%.1f", it.value),
                    )
                }
                    ?: sea.seaTemperature?.let {
                        stringResource(R.string.degrees, degrees(it.value))
                    }
                    ?: "–",
                listOfNotNull(
                        sea.seaTemperature?.let {
                            stringResource(
                                R.string.sea_temperature,
                                stringResource(R.string.degrees, degrees(it.value)),
                            )
                        },
                        sea.waveDirection?.let {
                            stringResource(R.string.waves_from, stringResource(Formats.compass(it)))
                        },
                        sea.wavePeriod?.let {
                            stringResource(R.string.wave_period, it.roundToInt())
                        },
                        sea.currentSpeed?.let {
                            stringResource(
                                R.string.sea_current,
                                String.format(java.util.Locale.getDefault(), "%.1f", it),
                            )
                        },
                    )
                    .joinToString("\n"),
                m,
            )
        }
    }

    extras.river?.let { river ->
        add { m ->
            Tile(
                stringResource(R.string.river),
                stringResource(R.string.discharge, Formats.flow(river.discharge)),
                listOfNotNull(
                        river.mean?.let { stringResource(Formats.riverLevel(river.discharge, it)) },
                        river.mean?.let {
                            stringResource(R.string.discharge_usual, Formats.flow(it))
                        },
                    )
                    .joinToString("\n"),
                m,
                source = stringResource(R.string.river_source),
            )
        }
    }

    extras.airQuality
        ?.pollen
        ?.takeIf { it.isNotEmpty() }
        ?.let { pollen ->
            val (worst, level) = pollen.maxBy { it.value }
            add { m ->
                Tile(
                    stringResource(R.string.pollen),
                    if (level < 1) stringResource(R.string.pollen_none)
                    else stringResource(Formats.pollenName(worst)),
                    if (level < 1) stringResource(R.string.pollen_none_caption)
                    else
                        pluralStringResource(
                            R.plurals.pollen_grains,
                            level.roundToInt(),
                            level.roundToInt(),
                        ),
                    m,
                    source = stringResource(R.string.air_source),
                )
            }
        }
}

private fun precipitationTile(next24: List<HourPoint>, todaySum: Double?): TileContent? {
    val amounts = next24.mapNotNull { it.precipitation?.value }
    if (amounts.isEmpty()) return null
    val total = amounts.sum()
    val mins = next24.mapNotNull { it.precipitationMin }
    val maxs = next24.mapNotNull { it.precipitationMax }
    val range = if (mins.isNotEmpty() && mins.size == maxs.size) mins.sum() to maxs.sum() else null
    return { m ->
        Tile(
            stringResource(R.string.precipitation),
            stringResource(R.string.mm, Formats.millimetres(total)),
            listOfNotNull(
                    stringResource(R.string.precipitation_next_24),
                    range?.let { (lo, hi) ->
                        stringResource(
                            R.string.precipitation_range,
                            Formats.millimetres(lo),
                            Formats.millimetres(hi),
                        )
                    },
                    todaySum?.let {
                        stringResource(R.string.precipitation_today, Formats.millimetres(it))
                    },
                )
                .joinToString("\n"),
            m,
        )
    }
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.prauga.weather.core.model.Place

@Composable
fun placeTitle(place: Place): String =
    when {
        place.name.isBlank() -> stringResource(R.string.current_location)
        place.isCurrentLocation && place.area != null -> "${place.name}, ${place.area}"
        else -> place.name
    }

fun placeDetail(place: Place): String =
    listOfNotNull(place.area, place.country).distinct().joinToString(", ")

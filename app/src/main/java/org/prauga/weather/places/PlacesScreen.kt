// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.places

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import org.prauga.weather.R
import org.prauga.weather.SubScreen
import org.prauga.weather.core.model.Place
import org.prauga.weather.placeDetail
import org.prauga.weather.placeTitle

private val LOCATION_PERMISSIONS =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

@Composable
fun PlacesScreen(viewModel: PlacesViewModel, onBack: (() -> Unit)?) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.closes.collect { onBack?.invoke() } }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            granted ->
            if (granted.values.any { it }) viewModel.useMyLocation() else viewModel.locationDenied()
        }
    SubScreen(stringResource(R.string.places), onBack, Arrangement.spacedBy(12.dp)) {
        item {
            PvotTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.search_places),
                singleLine = true,
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon =
                    if (state.query.isNotEmpty()) {
                        {
                            PvotIconButton(
                                painterResource(R.drawable.ic_close),
                                stringResource(R.string.clear_search),
                                { viewModel.onQueryChange("") },
                                colors = PvotIconButtonDefaults.plainColors(),
                            )
                        }
                    } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        when (val search = state.search) {
            SearchState.Idle -> {
                item {
                    PvotListGroup {
                        item {
                            PvotListRow(
                                title = stringResource(R.string.use_my_location),
                                supportingText =
                                    when {
                                        state.locating -> stringResource(R.string.locating)
                                        state.locateFailed -> stringResource(R.string.locate_failed)
                                        else -> null
                                    },
                                leading = {
                                    Icon(
                                        painterResource(R.drawable.ic_my_location),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    if (viewModel.hasLocationPermission) viewModel.useMyLocation()
                                    else permission.launch(LOCATION_PERMISSIONS)
                                },
                                enabled = !state.locating,
                            )
                        }
                    }
                }
                if (state.saved.isNotEmpty()) {
                    item {
                        PvotListGroup(title = stringResource(R.string.saved_places)) {
                            state.saved.forEach { place ->
                                item {
                                    SavedPlaceRow(place, place.id == state.selectedId, viewModel)
                                }
                            }
                        }
                    }
                }
            }
            SearchState.Searching -> item { Message(stringResource(R.string.searching)) }
            SearchState.Failed -> item { Message(stringResource(R.string.search_failed)) }
            is SearchState.Results ->
                if (search.places.isEmpty()) {
                    item { Message(stringResource(R.string.no_results)) }
                } else {
                    item {
                        PvotListGroup {
                            search.places.forEach { place ->
                                item {
                                    PvotListRow(
                                        title = place.name,
                                        supportingText = placeDetail(place),
                                        onClick = { viewModel.add(place) },
                                    )
                                }
                            }
                        }
                    }
                }
        }
        item { Message(stringResource(R.string.search_credit), Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun SavedPlaceRow(place: Place, selected: Boolean, viewModel: PlacesViewModel) {
    PvotListRow(
        title = placeTitle(place),
        supportingText = placeDetail(place).ifEmpty { null },
        leading =
            if (place.isCurrentLocation) {
                { Icon(painterResource(R.drawable.ic_my_location), contentDescription = null) }
            } else null,
        trailing = {
            PvotIconButton(
                painterResource(R.drawable.ic_delete),
                stringResource(R.string.remove_place, placeTitle(place)),
                { viewModel.remove(place) },
                colors = PvotIconButtonDefaults.plainColors(),
            )
        },
        selected = selected,
        onClick = { viewModel.select(place) },
    )
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

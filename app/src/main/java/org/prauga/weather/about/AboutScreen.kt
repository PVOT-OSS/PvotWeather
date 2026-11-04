// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import org.prauga.weather.BuildConfig
import org.prauga.weather.R
import org.prauga.weather.SubScreen
import org.prauga.weather.core.network.WeatherHttp

private data class Link(val title: Int, val summary: Int, val url: String)

private val sources =
    listOf(
        Link(
            R.string.about_open_meteo,
            R.string.about_open_meteo_summary,
            "https://open-meteo.com/",
        ),
        Link(R.string.about_met, R.string.about_met_summary, "https://api.met.no/"),
        Link(R.string.about_geonames, R.string.about_geonames_summary, "https://www.geonames.org/"),
        Link(
            R.string.about_era5,
            R.string.about_era5_summary,
            "https://cds.climate.copernicus.eu/",
        ),
        Link(
            R.string.about_cc_by,
            R.string.about_cc_by_summary,
            "https://creativecommons.org/licenses/by/4.0/",
        ),
    )

private val app =
    listOf(
        Link(R.string.about_source, R.string.about_source_summary, WeatherHttp.WEBSITE),
        Link(
            R.string.about_licence,
            R.string.about_licence_summary,
            "https://www.apache.org/licenses/LICENSE-2.0",
        ),
    )

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    SubScreen(stringResource(R.string.about), onBack, Arrangement.spacedBy(20.dp)) {
        item {
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Links(stringResource(R.string.about_data), sources) { uriHandler.openUri(it) } }
        item {
            Text(
                stringResource(R.string.about_disclaimer),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Links(stringResource(R.string.about_app), app) { uriHandler.openUri(it) } }
    }
}

@Composable
private fun Links(title: String, links: List<Link>, open: (String) -> Unit) {
    PvotListGroup(title = title) {
        links.forEach { link ->
            item {
                PvotListRow(
                    title = stringResource(link.title),
                    supportingText = stringResource(link.summary),
                    onClick = { open(link.url) },
                )
            }
        }
    }
}

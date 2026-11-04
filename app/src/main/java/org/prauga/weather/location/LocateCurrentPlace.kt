// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.location

import java.time.ZoneId
import org.prauga.weather.core.data.PlaceRepository
import org.prauga.weather.core.model.Place

class LocateCurrentPlace(private val finder: LocationFinder, private val places: PlaceRepository) {

    val hasPermission: Boolean
        get() = finder.hasPermission()

    suspend operator fun invoke(): Place? {
        val location = finder.locate() ?: return null
        val address = finder.describe(location)
        val name = address?.subLocality ?: address?.locality
        val area = if (address?.subLocality != null) address.locality else address?.adminArea
        val place =
            Place.of(
                name = name.orEmpty(),
                area = area,
                latitude = location.latitude,
                longitude = location.longitude,
                elevation = null,
                zone = ZoneId.systemDefault(),
                country = address?.countryName,
                isCurrentLocation = true,
            )
        val id = places.setCurrentLocation(place)
        return place.copy(id = id)
    }
}

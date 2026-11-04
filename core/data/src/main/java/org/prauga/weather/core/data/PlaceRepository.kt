// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.prauga.weather.core.data.cache.ForecastCacheDao
import org.prauga.weather.core.data.cache.PlaceDao
import org.prauga.weather.core.data.cache.PlaceEntity
import org.prauga.weather.core.model.Place

class PlaceRepository(private val places: PlaceDao, private val cache: ForecastCacheDao) {

    fun observeAll(): Flow<List<Place>> =
        places.observeAll().map { rows -> rows.map { it.toPlace() } }

    suspend fun get(id: Long): Place? = places.get(id)?.toPlace()

    suspend fun add(place: Place): Long =
        places.find(place.latitude, place.longitude)?.id
            ?: places.insert(
                place.toEntity(position = places.lastPosition() + 1, currentLocation = false)
            )

    suspend fun setCurrentLocation(place: Place): Long {
        val existing = places.currentLocation()
        if (existing == null) {
            return places.insert(place.toEntity(position = -1, currentLocation = true))
        }
        val moved = existing.latitude != place.latitude || existing.longitude != place.longitude
        places.update(place.toEntity(position = -1, currentLocation = true).copy(id = existing.id))
        if (moved) dropCacheIfUnused(existing)
        return existing.id
    }

    suspend fun remove(id: Long) {
        val place = places.get(id) ?: return
        places.delete(id)
        dropCacheIfUnused(place)
    }

    private suspend fun dropCacheIfUnused(place: PlaceEntity) {
        if (places.countAt(place.latitude, place.longitude) == 0) {
            cache.delete(place.toPlace().key)
        }
    }
}

internal fun PlaceEntity.toPlace() =
    Place(
        name = name,
        area = area,
        latitude = latitude,
        longitude = longitude,
        elevation = elevation,
        zone = ZoneId.of(zone),
        country = country,
        id = id,
        isCurrentLocation = currentLocation,
    )

internal fun Place.toEntity(position: Int, currentLocation: Boolean) =
    PlaceEntity(
        id = id,
        name = name,
        area = area,
        country = country,
        latitude = latitude,
        longitude = longitude,
        elevation = elevation,
        zone = zone.id,
        position = position,
        currentLocation = currentLocation,
    )

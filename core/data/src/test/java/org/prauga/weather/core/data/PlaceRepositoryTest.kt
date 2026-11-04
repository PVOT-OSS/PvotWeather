// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data

import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.weather.core.data.cache.CachedForecast
import org.prauga.weather.core.data.cache.ForecastCacheDao
import org.prauga.weather.core.data.cache.PlaceDao
import org.prauga.weather.core.data.cache.PlaceEntity
import org.prauga.weather.core.model.Place

class PlaceRepositoryTest {
    private val placeDao = FakePlaceDao()
    private val deletedCache = mutableListOf<String>()
    private val cacheDao =
        object : ForecastCacheDao {
            override fun observe(placeKey: String) = MutableStateFlow(emptyList<CachedForecast>())

            override suspend fun get(placeKey: String, provider: String) = null

            override suspend fun upsert(row: CachedForecast) = Unit

            override suspend fun touch(
                placeKey: String,
                provider: String,
                fetchedAt: Long,
                expiresAt: Long?,
            ) = Unit

            override suspend fun delete(placeKey: String) {
                deletedCache += placeKey
            }
        }
    private val repository = PlaceRepository(placeDao, cacheDao)
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val bengaluru =
        Place.of("Bengaluru", "Karnataka", 12.97194, 77.59369, 920, kolkata, "India")
    private val oslo =
        Place.of("Oslo", "Oslo", 59.91273, 10.74609, 26, ZoneId.of("Europe/Oslo"), "Norway")

    @Test
    fun addingTheSamePlaceTwiceKeepsOne() = runTest {
        val first = repository.add(bengaluru)
        assertEquals(first, repository.add(bengaluru))
        assertEquals(listOf("Bengaluru"), repository.observeAll().first().map { it.name })
    }

    @Test
    fun currentLocationComesFirstAndIsReplacedInPlace() = runTest {
        repository.add(oslo)
        val id =
            repository.setCurrentLocation(
                Place.of("KS Layout", "Bengaluru", 12.9, 77.5, null, kolkata)
            )
        val moved =
            repository.setCurrentLocation(
                Place.of("Jayanagar", "Bengaluru", 12.92, 77.58, null, kolkata)
            )
        assertEquals(id, moved)
        val places = repository.observeAll().first()
        assertEquals(listOf("Jayanagar", "Oslo"), places.map { it.name })
        assertTrue(places.first().isCurrentLocation)
        assertEquals(listOf("12.9,77.5"), deletedCache)
    }

    @Test
    fun removingAPlaceDropsItsCache() = runTest {
        val id = repository.add(oslo)
        repository.remove(id)
        assertTrue(repository.observeAll().first().isEmpty())
        assertEquals(listOf(oslo.key), deletedCache)
    }

    private class FakePlaceDao : PlaceDao {
        private val rows = MutableStateFlow(listOf<PlaceEntity>())
        private var nextId = 1L

        override fun observeAll() =
            rows.map { list ->
                list.sortedWith(
                    compareByDescending<PlaceEntity> { it.currentLocation }.thenBy { it.position }
                )
            }

        override suspend fun get(id: Long) = rows.value.find { it.id == id }

        override suspend fun currentLocation() = rows.value.find { it.currentLocation }

        override suspend fun find(latitude: Double, longitude: Double) =
            rows.value.find {
                !it.currentLocation && it.latitude == latitude && it.longitude == longitude
            }

        override suspend fun lastPosition() = rows.value.maxOfOrNull { it.position } ?: -1

        override suspend fun countAt(latitude: Double, longitude: Double) =
            rows.value.count { it.latitude == latitude && it.longitude == longitude }

        override suspend fun insert(place: PlaceEntity): Long {
            val id = nextId++
            rows.value += place.copy(id = id)
            return id
        }

        override suspend fun update(place: PlaceEntity) {
            rows.value = rows.value.map { if (it.id == place.id) place else it }
        }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }
}

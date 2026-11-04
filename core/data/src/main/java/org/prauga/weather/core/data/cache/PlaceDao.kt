// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY current_location DESC, position, id")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id") suspend fun get(id: Long): PlaceEntity?

    @Query("SELECT * FROM places WHERE current_location = 1 LIMIT 1")
    suspend fun currentLocation(): PlaceEntity?

    @Query(
        "SELECT * FROM places WHERE current_location = 0 AND latitude = :latitude AND longitude = :longitude LIMIT 1"
    )
    suspend fun find(latitude: Double, longitude: Double): PlaceEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM places") suspend fun lastPosition(): Int

    @Query("SELECT COUNT(*) FROM places WHERE latitude = :latitude AND longitude = :longitude")
    suspend fun countAt(latitude: Double, longitude: Double): Int

    @Insert suspend fun insert(place: PlaceEntity): Long

    @Update suspend fun update(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id") suspend fun delete(id: Long)
}

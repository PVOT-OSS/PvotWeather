// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data.cache

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ForecastCacheDao {
    @Query("SELECT * FROM forecast_cache WHERE place_key = :placeKey")
    fun observe(placeKey: String): Flow<List<CachedForecast>>

    @Query("SELECT * FROM forecast_cache WHERE place_key = :placeKey AND provider = :provider")
    suspend fun get(placeKey: String, provider: String): CachedForecast?

    @Upsert suspend fun upsert(row: CachedForecast)

    @Query(
        "UPDATE forecast_cache SET expires_at = :expiresAt, fetched_at = :fetchedAt " +
            "WHERE place_key = :placeKey AND provider = :provider"
    )
    suspend fun touch(placeKey: String, provider: String, fetchedAt: Long, expiresAt: Long?)

    @Query("DELETE FROM forecast_cache WHERE place_key = :placeKey")
    suspend fun delete(placeKey: String)
}

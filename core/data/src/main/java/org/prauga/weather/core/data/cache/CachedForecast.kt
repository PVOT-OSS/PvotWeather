// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data.cache

import androidx.room.Entity

@Entity(tableName = "forecast_cache", primaryKeys = ["place_key", "provider"])
data class CachedForecast(
    @androidx.room.ColumnInfo(name = "place_key") val placeKey: String,
    val provider: String,
    val body: String,
    @androidx.room.ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    @androidx.room.ColumnInfo(name = "expires_at") val expiresAt: Long?,
    @androidx.room.ColumnInfo(name = "last_modified") val lastModified: String?,
)

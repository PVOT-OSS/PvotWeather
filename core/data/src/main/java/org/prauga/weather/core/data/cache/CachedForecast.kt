// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data.cache

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "forecast_cache", primaryKeys = ["place_key", "provider"])
data class CachedForecast(
    @ColumnInfo(name = "place_key") val placeKey: String,
    val provider: String,
    val body: String,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long?,
    @ColumnInfo(name = "last_modified") val lastModified: String?,
)

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.data.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val area: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int?,
    val zone: String,
    val position: Int,
    @ColumnInfo(name = "current_location") val currentLocation: Boolean,
)

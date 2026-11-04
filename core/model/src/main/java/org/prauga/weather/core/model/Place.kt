// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId

data class Place(
    val name: String,
    val area: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int?,
    val zone: ZoneId,
    val country: String? = null,
    val id: Long = 0,
    val isCurrentLocation: Boolean = false,
) {
    val key: String
        get() = "${format(latitude)},${format(longitude)}"

    companion object {
        fun of(
            name: String,
            area: String?,
            latitude: Double,
            longitude: Double,
            elevation: Int?,
            zone: ZoneId,
            country: String? = null,
            id: Long = 0,
            isCurrentLocation: Boolean = false,
        ) =
            Place(
                name,
                area,
                truncate(latitude),
                truncate(longitude),
                elevation,
                zone,
                country,
                id,
                isCurrentLocation,
            )

        fun truncate(value: Double): Double =
            BigDecimal(value.toString()).setScale(4, RoundingMode.DOWN).toDouble()

        fun format(value: Double): String =
            BigDecimal(value.toString())
                .setScale(4, RoundingMode.DOWN)
                .stripTrailingZeros()
                .toPlainString()
    }
}

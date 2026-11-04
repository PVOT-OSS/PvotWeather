// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.core.network

internal object Fixtures {
    fun read(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name.json")) { name }
            .readText()
}

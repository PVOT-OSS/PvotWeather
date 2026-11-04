// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather

import android.app.Application

class WeatherApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

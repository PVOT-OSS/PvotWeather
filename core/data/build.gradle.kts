// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.pvot.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android { namespace = "org.prauga.weather.core.data" }

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    api(project(":core:model"))
    api(project(":core:network"))
    api(libs.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.room.compiler)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}

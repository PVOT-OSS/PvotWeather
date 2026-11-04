// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.weather.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationFinder(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean = granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun granted(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun locate(): Location? {
        if (!hasPermission()) return null
        val providers = providers()
        lastKnown(providers)
            ?.takeIf { System.currentTimeMillis() - it.time < RECENT_MS }
            ?.let {
                return it
            }
        for (provider in providers) {
            withTimeoutOrNull(FIX_TIMEOUT_MS) { current(provider) }
                ?.let {
                    return it
                }
        }
        return lastKnown(providers)
    }

    suspend fun describe(location: Location): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) =
                                cont.resume(addresses.firstOrNull())

                            override fun onError(errorMessage: String?) = cont.resume(null)
                        },
                    )
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                        geocoder
                            .getFromLocation(location.latitude, location.longitude, 1)
                            ?.firstOrNull()
                    }
                    .getOrNull()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(providers: List<String>): Location? =
        providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }

    private fun providers(): List<String> =
        buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    add(LocationManager.FUSED_PROVIDER)
                add(LocationManager.NETWORK_PROVIDER)
                if (granted(Manifest.permission.ACCESS_FINE_LOCATION))
                    add(LocationManager.GPS_PROVIDER)
            }
            .filter { it in manager.allProviders && manager.isProviderEnabled(it) }

    @SuppressLint("MissingPermission")
    private suspend fun current(provider: String): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            manager.getCurrentLocation(provider, signal, context.mainExecutor) { cont.resume(it) }
        } else {
            val listener = LocationListener { if (cont.isActive) cont.resume(it) }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
            @Suppress("DEPRECATION")
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val FIX_TIMEOUT_MS = 20_000L
        const val RECENT_MS = 10 * 60_000L
    }
}

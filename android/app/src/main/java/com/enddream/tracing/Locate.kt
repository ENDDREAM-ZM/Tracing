package com.enddream.tracing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object Locate {

    fun cityName(ctx: Context, lat: Double, lng: Double): String? = runCatching {
        val geocoder = android.location.Geocoder(
            ctx,
            java.util.Locale.getDefault()
        )
        @Suppress("DEPRECATION")
        val list = geocoder.getFromLocation(lat, lng, 1).orEmpty()
        val city = list.firstOrNull()?.locality
            ?: list.firstOrNull()?.subAdminArea
            ?: list.firstOrNull()?.adminArea
        city?.trim()?.let { if (it.endsWith("市")) it else "${it}市" }
    }.getOrNull()

    fun candidates(ctx: Context, lat: Double, lng: Double): List<String> = runCatching {
        val geocoder = android.location.Geocoder(ctx, java.util.Locale.CHINA)
        @Suppress("DEPRECATION")
        val a = geocoder.getFromLocation(lat, lng, 1).orEmpty().firstOrNull() ?: return@runCatching emptyList()
        listOfNotNull(a.subLocality, a.locality, a.subAdminArea, a.adminArea)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    fun granted(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(lm: LocationManager): Location? = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    ).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        .maxByOrNull { it.time }

    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context, timeoutMs: Long = 6000L): Pair<Double, Double>? {
        if (!granted(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val cached = lastKnown(lm)
        val fresh = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location)
                    }

                    override fun onProviderEnabled(provider: String) {}

                    override fun onProviderDisabled(provider: String) {}

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
                val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                    .filter { p -> runCatching { lm.isProviderEnabled(p) }.getOrDefault(false) }
                if (providers.isEmpty()) {
                    if (cont.isActive) cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                runCatching {
                    lm.requestLocationUpdates(
                        providers.first(),
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            }
        }
        val best = listOfNotNull(cached, fresh).maxByOrNull { it.time } ?: return null
        return best.longitude to best.latitude
    }
}

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
import com.enddream.tracing.data.Prefs
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object Locate {

    data class Pos(val lng: Double, val lat: Double)

    @Volatile
    private var agreeCache: Pair<Long, Boolean>? = null

    suspend fun enabled(ctx: Context): Boolean {
        val now = System.currentTimeMillis()
        agreeCache?.let { (t, v) -> if (now - t < 10_000) return v }
        val v = runCatching { Prefs.amapAgree(ctx) }.getOrDefault(false) &&
            !AmapLoc.isBroken(ctx)
        agreeCache = now to v
        return v
    }

    private var lastFix: Pair<Long, Pos>? = null

    fun clearCache() {
        agreeCache = null
        cachedCity = null
        geocoderCity = null
        lastFix = null
    }

    fun granted(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    suspend fun current(
        ctx: Context,
        timeoutMs: Long = 7000L,
        maxAgeMs: Long = 60_000L
    ): Pos? {
        if (!granted(ctx)) return null
        val now = System.currentTimeMillis()
        lastFix?.let { (t, p) -> if (now - t <= maxAgeMs) return p }
        val amap = if (enabled(ctx)) runCatching { AmapLoc.once(ctx, timeoutMs) }.getOrNull() else null
        if (amap != null) {
            cachedCity = now to cityList(amap)
            val p = Pos(amap.lng, amap.lat)
            lastFix = now to p
            return p
        }
        val sys = systemFix(ctx, timeoutMs) ?: return null
        val (la, ln) = AmapLoc.toGcj02(ctx, sys.second, sys.first)
        val p = Pos(ln, la)
        lastFix = now to p
        return p
    }

    suspend fun candidates(ctx: Context, lat: Double, lng: Double): List<String> {
        val now = System.currentTimeMillis()
        cachedCity?.let { (t, v) -> if (now - t < 5 * 60 * 1000 && v.isNotEmpty()) return v }
        val amap = if (enabled(ctx)) runCatching { AmapLoc.regeo(ctx, lat, lng) }.getOrNull() else null
        val fromAmap = if (amap != null) cityList(amap) else emptyList()
        val r = if (fromAmap.isNotEmpty()) fromAmap else geocoderCandidates(ctx, lat, lng)
        if (r.isNotEmpty()) cachedCity = now to r
        return r
    }

    private fun cityList(f: AmapLoc.Fix): List<String> = listOfNotNull(
        f.district,
        f.city?.let { if (it.endsWith("市")) it else "${it}市" },
        f.city,
        f.aoi
    ).map { it.trim() }.filter { it.isNotBlank() }.distinct()

    private fun geocoderCandidates(ctx: Context, lat: Double, lng: Double): List<String> {
        val now = System.currentTimeMillis()
        geocoderCity?.let { (t, v) -> if (now - t < 5 * 60 * 1000) return v }
        val r = runCatching {
            val geocoder = android.location.Geocoder(ctx, java.util.Locale.CHINA)
            @Suppress("DEPRECATION")
            val a = geocoder.getFromLocation(lat, lng, 1).orEmpty().firstOrNull()
                ?: return@runCatching emptyList()
            listOfNotNull(a.subLocality, a.locality, a.subAdminArea, a.adminArea)
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
        if (r.isNotEmpty()) geocoderCity = now to r
        return r
    }

    private var cachedCity: Pair<Long, List<String>>? = null
    private var geocoderCity: Pair<Long, List<String>>? = null

    @SuppressLint("MissingPermission")
    private fun lastKnown(lm: LocationManager): List<Location> = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    ).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        .filter { usable(it) }

    private fun usable(l: Location): Boolean {
        val age = System.currentTimeMillis() - l.time
        if (age > 3 * 60 * 1000) return false
        if (!l.hasAccuracy()) return true
        return l.accuracy <= 500f
    }

    @SuppressLint("MissingPermission")
    suspend fun systemFix(ctx: Context, timeoutMs: Long = 3000L): Pair<Double, Double>? {
        if (!granted(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val cached = lastKnown(lm)
        val now = System.currentTimeMillis()
        val quick = cached
            .filter { now - it.time <= 60_000 && accuracy(it) <= 200f }
            .minByOrNull { accuracy(it) }
        if (quick != null) return quick.longitude to quick.latitude
        var bestFix: Location? = cached.minByOrNull { accuracy(it) }
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Unit> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val cur = bestFix
                        if (cur == null || accuracy(location) < accuracy(cur)) bestFix = location
                        if (location.hasAccuracy() && location.accuracy <= 100f) {
                            runCatching { lm.removeUpdates(this) }
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }

                    override fun onProviderEnabled(provider: String) {}

                    override fun onProviderDisabled(provider: String) {}

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    .filter { p -> runCatching { lm.isProviderEnabled(p) }.getOrDefault(false) }
                if (providers.isEmpty()) {
                    if (cont.isActive) cont.resume(Unit)
                    return@suspendCancellableCoroutine
                }
                providers.forEach { p ->
                    runCatching {
                        lm.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper())
                    }
                }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            }
        }
        val fresh = bestFix
        val pool = if (fresh != null) cached + fresh else cached
        if (pool.isEmpty()) return null
        val best = pool.minByOrNull { accuracy(it) } ?: return null
        return best.longitude to best.latitude
    }

    private fun accuracy(l: Location): Float = if (l.hasAccuracy()) l.accuracy else 9999f
}

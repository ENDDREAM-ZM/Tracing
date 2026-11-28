package com.enddream.tracing

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.CoordinateConverter
import com.amap.api.location.DPoint
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object AmapLoc {

    data class Fix(
        val lat: Double,
        val lng: Double,
        val accuracy: Float,
        val city: String?,
        val district: String?,
        val aoi: String?,
        val address: String?,
        val adcode: String?
    )

    private const val SAFE = "amap_safe"

    private val lock = Mutex()

    @Volatile
    private var ready = false

    @Volatile
    private var pending: CancellableContinuation<Fix?>? = null

    fun markInflight(ctx: Context) {
        runCatching {
            ctx.getSharedPreferences(SAFE, Context.MODE_PRIVATE).edit()
                .putBoolean("inflight", true).apply()
        }
    }

    fun clearInflight(ctx: Context) {
        runCatching {
            ctx.getSharedPreferences(SAFE, Context.MODE_PRIVATE).edit()
                .putBoolean("inflight", false).apply()
        }
    }

    fun wasInflight(ctx: Context): Boolean = runCatching {
        ctx.getSharedPreferences(SAFE, Context.MODE_PRIVATE).getBoolean("inflight", false)
    }.getOrDefault(false)

    fun markBroken(ctx: Context) {
        runCatching {
            ctx.getSharedPreferences(SAFE, Context.MODE_PRIVATE).edit()
                .putBoolean("broken", true).apply()
        }
    }

    fun clearBroken(ctx: Context) {
        runCatching {
            ctx.getSharedPreferences(SAFE, Context.MODE_PRIVATE).edit()
                .putBoolean("broken", false).apply()
        }
    }

    fun isBroken(ctx: Context): Boolean = runCatching {
        ctx.getSharedPreferences(SAFE, Context.MODE_PRIVATE).getBoolean("broken", false)
    }.getOrDefault(false)

    fun init(ctx: Context) {
        if (ready) return
        ready = true
        runCatching {
            val app = ctx.applicationContext
            AMapLocationClient.updatePrivacyShow(app, true, true)
            AMapLocationClient.updatePrivacyAgree(app, true)
            AMapLocationClientOption.setLocationProtocol(
                AMapLocationClientOption.AMapLocationProtocol.HTTPS
            )
        }
    }

    private fun option(timeout: Long): AMapLocationClientOption =
        AMapLocationClientOption().apply {
            setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
            setOnceLocation(true)
            setOnceLocationLatest(true)
            setNeedAddress(true)
            setMockEnable(false)
            setLocationCacheEnable(true)
            setHttpTimeOut(timeout.coerceIn(3000L, 15000L))
            setGeoLanguage(AMapLocationClientOption.GeoLanguage.ZH)
        }

    private fun fixOf(a: AMapLocation): Fix = Fix(
        lat = a.latitude,
        lng = a.longitude,
        accuracy = a.accuracy,
        city = a.city?.trim()?.takeIf { it.isNotBlank() },
        district = a.district?.trim()?.takeIf { it.isNotBlank() },
        aoi = a.aoiName?.trim()?.takeIf { it.isNotBlank() },
        address = a.address?.trim()?.takeIf { it.isNotBlank() },
        adcode = a.adCode?.trim()?.takeIf { it.isNotBlank() }
    )

    private val main = Handler(Looper.getMainLooper())

    private fun post(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runCatching(block)
        } else {
            main.post { runCatching(block) }
        }
    }

    private suspend fun locClientOf(ctx: Context): AMapLocationClient? {
        var made: AMapLocationClient? = null
        withContext(Dispatchers.Main.immediate) {
            made = runCatching {
                AMapLocationClient(Looper.getMainLooper(), ctx.applicationContext).also { c ->
                    c.setLocationListener { loc ->
                        val cont = pending ?: return@setLocationListener
                        if (!cont.isActive) return@setLocationListener
                        val fix = if (loc != null && loc.errorCode == 0) {
                            runCatching { fixOf(loc) }.getOrNull()
                        } else {
                            null
                        }
                        cont.resume(fix)
                    }
                }
            }.getOrNull()
        }
        return made
    }

    private suspend fun geoClientOf(ctx: Context): AMapLocationClient? {
        var made: AMapLocationClient? = null
        withContext(Dispatchers.Main.immediate) {
            made = runCatching {
                AMapLocationClient(Looper.getMainLooper(), ctx.applicationContext).also { c ->
                    c.setReGeoLocationCallback { loc ->
                        val cont = pending ?: return@setReGeoLocationCallback
                        if (!cont.isActive) return@setReGeoLocationCallback
                        val fix = if (loc != null && loc.errorCode == 0) {
                            runCatching { fixOf(loc) }.getOrNull()
                        } else {
                            null
                        }
                        cont.resume(fix)
                    }
                }
            }.getOrNull()
        }
        return made
    }

    suspend fun once(ctx: Context, timeoutMs: Long = 8000L): Fix? = lock.withLock {
        markInflight(ctx)
        init(ctx)
        val client = locClientOf(ctx)
        if (client == null) {
            return@withLock null
        }
        val fix = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Fix?> { cont ->
                pending = cont
                post {
                    client.setLocationOption(option(timeoutMs))
                    client.startLocation()
                }
            }
        }
        pending = null
        post {
            client.stopLocation()
            client.onDestroy()
        }
        clearInflight(ctx)
        fix
    }

    suspend fun regeo(ctx: Context, lat: Double, lng: Double, timeoutMs: Long = 6000L): Fix? =
        lock.withLock {
            markInflight(ctx)
            init(ctx)
            val client = geoClientOf(ctx)
            if (client == null) return@withLock null
            val fix = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Fix?> { cont ->
                    pending = cont
                    post {
                        val seed = Location("amap").apply {
                            latitude = lat
                            longitude = lng
                        }
                        client.getReGeoLocation(AMapLocation(seed))
                    }
                }
            }
            pending = null
            post { client.onDestroy() }
            clearInflight(ctx)
            fix
        }

    fun toGcj02(ctx: Context, lat: Double, lng: Double): Pair<Double, Double> {
        if (lat == 0.0 || lng == 0.0) return lat to lng
        init(ctx)
        val sdk = runCatching {
            val c = CoordinateConverter(ctx.applicationContext)
            c.from(CoordinateConverter.CoordType.GPS)
            c.coord(DPoint(lat, lng))
            val p = c.convert()
            p.latitude to p.longitude
        }.getOrNull()
        if (sdk != null && sdk.first != 0.0) return sdk
        return localGcj02(lat, lng)
    }

    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    private fun outOfChina(lat: Double, lng: Double): Boolean =
        lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271

    private fun transformLat(x: Double, y: Double): Double {
        var r = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
        r += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        r += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0
        r += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320.0 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0
        return r
    }

    private fun transformLng(x: Double, y: Double): Double {
        var r = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
        r += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        r += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0
        r += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0
        return r
    }

    fun localGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * Math.PI
        var magic = Math.sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = Math.sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * Math.PI)
        return (lat + dLat) to (lng + dLng)
    }

    fun distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float =
        runCatching {
            CoordinateConverter.calculateLineDistance(
                DPoint(lat1, lng1),
                DPoint(lat2, lng2)
            )
        }.getOrDefault(-1f)
}

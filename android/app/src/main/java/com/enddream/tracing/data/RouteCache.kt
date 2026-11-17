package com.enddream.tracing.data

import com.enddream.tracing.data.api.BusApi
import com.enddream.tracing.data.api.RouteDetail

object RouteCache {

    private const val LIMIT = 240

    private val details = object : LinkedHashMap<String, RouteDetail?>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RouteDetail?>?): Boolean =
            size > LIMIT
    }

    private fun key(city: String, line: String, dir: Int) = "$city|$line|$dir"

    fun cached(city: String, line: String, dir: Int): RouteDetail? = synchronized(details) {
        details[key(city, line, dir)]
    }

    suspend fun get(api: BusApi, city: String, line: String, dir: Int): RouteDetail? {
        cached(city, line, dir)?.let { return it }
        val d = runCatching { api.routeDetail(city, line, dir) }.getOrNull()
        synchronized(details) { details[key(city, line, dir)] = d }
        return d
    }

    fun matchesNear(d: RouteDetail?, stationName: String, lng: Double, lat: Double): Boolean {
        if (d == null) return true
        val hits = d.data.filter { it.stationName == stationName || it.showName == stationName }
        if (hits.isEmpty()) return true
        return hits.any {
            val m = Geo.meters(it.lat, it.lon, lat, lng)
            m < 0 || m <= 800.0
        }
    }
}

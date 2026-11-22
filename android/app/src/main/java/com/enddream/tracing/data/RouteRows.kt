package com.enddream.tracing.data

import kotlin.math.abs

sealed interface RailRow {
    val key: String

    data class Station(
        val order: Int,
        val name: String,
        val first: Boolean,
        val last: Boolean,
        val before: String,
        val after: String,
        val times: List<String> = emptyList(),
        val transfers: List<MetroTransfer> = emptyList(),
        val si: String = "",
        val lat: Double = 0.0,
        val lng: Double = 0.0,
        val door: String = ""
    ) : RailRow {
        override val key: String get() = "s-$order"
    }

    data class Vehicle(
        val id: String,
        val label: String,
        val detail: String,
        val color: String,
        val atStation: Boolean
    ) : RailRow {
        override val key: String get() = "v-$id"
    }
}

object Geo {
    fun meters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if (lat1 == 0.0 || lng1 == 0.0 || lat2 == 0.0 || lng2 == 0.0) return -1.0
        val r = 6371000.0
        val p1 = lat1 * Math.PI / 180
        val p2 = lat2 * Math.PI / 180
        val dp = (lat2 - lat1) * Math.PI / 180
        val dl = (lng2 - lng1) * Math.PI / 180
        val a = Math.sin(dp / 2) * Math.sin(dp / 2) +
            Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2)
        return 2 * r * Math.asin(Math.sqrt(a))
    }
}

object RouteRows {

    fun build(
        detail: com.enddream.tracing.data.api.RouteDetail?,
        buses: List<com.enddream.tracing.data.api.RealBus>,
        speeds: List<com.enddream.tracing.data.api.SpeedItem>,
        etas: Map<String, String> = emptyMap()
    ): List<RailRow> {
        val stations = detail?.data.orEmpty()
        if (stations.isEmpty()) return emptyList()
        val n = stations.size
        val segColor = { j: Int -> speeds.getOrNull(j)?.co.orEmpty() }

        val arrived = HashMap<Int, MutableList<com.enddream.tracing.data.api.RealBus>>()
        val moving = HashMap<Int, MutableList<com.enddream.tracing.data.api.RealBus>>()
        buses.forEach { b ->
            if (b.index < 0) return@forEach
            if (b.statusType == "0") {
                arrived.getOrPut(b.index.coerceIn(0, n - 1)) { mutableListOf() }.add(b)
            } else {
                moving.getOrPut((b.index - 1).coerceIn(0, n - 1)) { mutableListOf() }.add(b)
            }
        }

        val out = ArrayList<RailRow>(n * 2)
        for (p in 0 until n) {
            val st = stations[p]
            out.add(
                RailRow.Station(
                    order = p,
                    name = st.stationName.ifBlank { st.showName },
                    first = p == 0,
                    last = p == n - 1,
                    before = if (p == 0) "none" else segColor(p - 1),
                    after = if (p == n - 1) "none" else segColor(p),
                    lat = st.lat,
                    lng = st.lon
                )
            )
            arrived[p]?.forEach { b ->
                out.add(
                    RailRow.Vehicle(
                        id = "at-$p-${b.busNumber}",
                        label = b.busNumber.ifBlank { "车辆" },
                        detail = "到站",
                        color = segColor(p.coerceAtMost((n - 2).coerceAtLeast(0))),
                        atStation = true
                    )
                )
            }
            moving[p]?.forEach { b ->
                out.add(
                    RailRow.Vehicle(
                        id = "mv-$p-${b.busNumber}",
                        label = b.busNumber.ifBlank { "车辆" },
                        detail = listOfNotNull(
                            "行驶中",
                            etas[b.busNumber]?.takeIf { it.isNotBlank() }
                                ?: etaText(b.busToStationNiheDistance, speeds, p),
                            "${b.busToStationNiheDistance.toInt()}m"
                        ).joinToString(" · "),
                        color = segColor(p),
                        atStation = false
                    )
                )
            }
        }
        return out
    }

    private fun etaText(distance: Double, speeds: List<com.enddream.tracing.data.api.SpeedItem>, seg: Int): String? {
        if (distance <= 0.0) return null
        val v = speeds.getOrNull(seg)?.speed ?: 0.0
        if (v <= 0.0) return null
        val minutes = distance / v
        return when {
            minutes < 1.0 -> "小于1分钟"
            minutes < 60.0 -> "${Math.round(minutes)}分钟"
            else -> "${Math.round(minutes) / 60}小时${Math.round(minutes) % 60}分钟"
        }
    }

    fun arrivedStations(buses: List<com.enddream.tracing.data.api.RealBus>): Set<String> =
        buses.filter { it.statusType == "0" }
            .map { it.stationName }
            .filter { it.isNotBlank() }
            .toSet()

    fun approachingStations(
        buses: List<com.enddream.tracing.data.api.RealBus>,
        detail: com.enddream.tracing.data.api.RouteDetail?
    ): Set<String> {
        val stations = detail?.data.orEmpty()
        if (stations.isEmpty()) return emptySet()
        return buses.filter { it.statusType != "0" && it.busToStationNiheDistance <= 600.0 }
            .mapNotNull { stations.getOrNull(it.index) }
            .map { it.stationName.ifBlank { it.showName } }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun firstStation(detail: com.enddream.tracing.data.api.RouteDetail?): String =
        detail?.data?.firstOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()

    fun lastStation(detail: com.enddream.tracing.data.api.RouteDetail?): String =
        detail?.data?.lastOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()
}

object MetroTimetable {

    private const val PEAK_HEADWAY = 5
    private const val OFF_PEAK_HEADWAY = 8

    fun isPeak(minuteOfDay: Int): Boolean {
        val m = minuteOfDay
        return (m in 7 * 60..9 * 60 + 30) || (m in 17 * 60..19 * 60 + 30)
    }

    fun parseClock(text: String): Int? = toMin(text)

    private fun toMin(text: String): Int? {
        val t = text.trim()
        if (!t.contains(":")) return null
        val nextDay = t.startsWith("次日")
        val core = t.removePrefix("次日").trim().substringBefore("*").trim()
        val h = core.substringBefore(":").toIntOrNull() ?: return null
        val m = core.substringAfter(":").toIntOrNull() ?: return null
        return h * 60 + m + if (nextDay) 24 * 60 else 0
    }

    private fun fmt(min: Int): String {
        val h = (min / 60) % 24
        val m = min % 60
        return String.format("%02d:%02d", h, m)
    }

    fun timeline(first: String, last: String, peak: Boolean): List<String> {
        val f = toMin(first) ?: return emptyList()
        val l = toMin(last) ?: return emptyList()
        if (l < f) return emptyList()
        val headway = if (peak) PEAK_HEADWAY else OFF_PEAK_HEADWAY
        val out = ArrayList<String>()
        var t = f
        var guard = 0
        while (t <= l && guard < 400) {
            guard++
            out.add(fmt(t))
            t += headway
        }
        return out
    }

    fun twoArrivals(
        first: String,
        last: String,
        nowMinute: Int,
        peak: Boolean
    ): List<String> {
        val f = toMin(first) ?: return emptyList()
        val l = toMin(last) ?: return emptyList()
        if (l < f) return emptyList()
        if (nowMinute < f || nowMinute > l) return emptyList()
        val headway = if (peak) PEAK_HEADWAY else OFF_PEAK_HEADWAY
        val k = if (nowMinute <= f) 0 else (nowMinute - f) / headway
        val arrivedAt = f + k * headway
        val dwelling = nowMinute - arrivedAt in 0..1
        val out = ArrayList<String>(2)
        if (dwelling) out.add("即将到达")
        var t = arrivedAt + headway
        var guard = 0
        while (out.size < 2 && t <= l && guard < 64) {
            guard++
            out.add(deltaText(t - nowMinute))
            t += headway
        }
        return out
    }

    fun deltaText(minutes: Int): String =
        if (minutes <= 0) "即将到达" else "$minutes 分钟"

    fun serviceWindow(times: List<MetroTime>): Pair<Int, Int>? {
        val f = times.mapNotNull { toMin(it.first) }
        val l = times.mapNotNull { toMin(it.last) }
        if (f.isEmpty() || l.isEmpty()) return null
        return f.min() to l.max()
    }

    fun firstLastOf(times: List<MetroTime>): Pair<String, String>? {
        val firsts = times.mapNotNull { toMin(it.first) }.filter { it < 24 * 60 }
        val lasts = times.mapNotNull { toMin(it.last) }.filter { it < 24 * 60 }
        if (firsts.isEmpty() || lasts.isEmpty()) return null
        return fmt(firsts.min()) to fmt(lasts.max())
    }

    fun formatClock(min: Int): String = fmt(min)

    fun offsetFromServer(serverMillis: Long): Long {
        if (serverMillis <= 0L) return 0L
        val diff = serverMillis - System.currentTimeMillis()
        return if (abs(diff) > 24L * 3600 * 1000) 0L else diff
    }
}

package com.enddream.tracing.data

import kotlin.math.max

data class MetroTrain(
    val id: String,
    val segIndex: Int,
    val atStation: Boolean,
    val label: String,
    val detail: String
)

object MetroPredict {

    private const val DWELL_SEC = 35.0
    private const val SPEED_MPS = 12.0

    private fun segLen(a: MetroStationRow, b: MetroStationRow): Double {
        val d = Geo.meters(a.lat, a.lng, b.lat, b.lng)
        return if (d <= 0) 900.0 else d
    }

    private fun cumTimes(stations: List<MetroStationRow>): DoubleArray {
        val t = DoubleArray(stations.size)
        var acc = 0.0
        for (i in 0 until stations.size - 1) {
            acc += segLen(stations[i], stations[i + 1]) / SPEED_MPS + DWELL_SEC
            t[i + 1] = acc
        }
        return t
    }

    private fun text(seconds: Double): String {
        if (seconds <= 0.0) return "列车已到站"
        val m = kotlin.math.ceil(seconds / 60.0).toInt()
        return if (m <= 1) "即将到达" else "$m 分钟"
    }

    fun trains(
        stations: List<MetroStationRow>,
        minuteOfDay: Double,
        serviceStart: Int,
        serviceEnd: Int,
        headwayMin: Double,
        limit: Int = 2
    ): List<MetroTrain> {
        if (stations.size < 2) return emptyList()
        if (minuteOfDay < serviceStart || minuteOfDay > serviceEnd) return emptyList()
        val cum = cumTimes(stations)
        val total = cum.last()
        if (total <= 0) return emptyList()
        val running = max(1, (total / 60.0 / headwayMin).toInt()).coerceAtMost(16)
        val elapsed = (minuteOfDay - serviceStart) * 60.0
        val out = ArrayList<MetroTrain>(running)
        for (k in 0 until running) {
            val phase = (elapsed + k * headwayMin * 60.0) % total
            var i = 0
            while (i < stations.size - 2 && cum[i + 1] <= phase) i++
            val dwellEnd = cum[i] + DWELL_SEC
            if (phase < dwellEnd) {
                out.add(MetroTrain("t$k-at-$i", i, true, "", "列车已到站"))
            } else {
                val remain = (cum[i + 1] - phase).coerceAtLeast(0.0)
                out.add(MetroTrain("t$k-mv-$i", i, false, "", text(remain)))
            }
        }
        return out.sortedBy { it.segIndex }.take(limit)
    }

    fun rows(
        stations: List<MetroStationRow>,
        minuteOfDay: Int,
        color: String,
        times: Map<String, List<MetroTime>>,
        lineIdMap: Map<String, String>,
        selfLineIds: Set<String>,
        reverse: Boolean,
        lineColors: Map<String, String> = emptyMap()
    ): List<RailRow> {
        if (stations.isEmpty()) return emptyList()
        val ordered = if (reverse) stations.reversed() else stations
        val peak = MetroTimetable.isPeak(minuteOfDay)
        val selfNames = selfLineIds.mapNotNull { lineIdMap[it] }.toSet()
        val terminalSi = if (reverse) stations.first().si else stations.last().si
        val lineTimes = stations.flatMap { st ->
            times[st.si].orEmpty().filter { selfLineIds.isEmpty() || it.lineId in selfLineIds }
        }
        val window = MetroTimetable.serviceWindow(lineTimes)
        val start = window?.first ?: 6 * 60
        val end = window?.second ?: 23 * 60
        val inService = minuteOfDay in start..end
        val headway = if (peak) 5.0 else 8.0
        val trains = if (inService) {
            trains(ordered, minuteOfDay.toDouble(), start, end, headway, 2)
        } else {
            emptyList()
        }
        val out = ArrayList<RailRow>(ordered.size * 2)

        ordered.forEachIndexed { i, st ->
            val entry = times[st.si].orEmpty()
                .filter { selfLineIds.isEmpty() || it.lineId in selfLineIds }
            val directed = entry.filter { it.toward == terminalSi }
            val pick = if (directed.isNotEmpty()) directed else entry
            val countdown = ArrayList<String>(2)
            if (inService) {
                pick.forEach { t ->
                    MetroTimetable.twoArrivals(t.first, t.last, minuteOfDay, peak).forEach { c ->
                        if (countdown.size < 2) countdown.add(c)
                    }
                }
            }
            out.add(
                RailRow.Station(
                    order = i,
                    name = st.name,
                    first = i == 0,
                    last = i == ordered.lastIndex,
                    before = color,
                    after = color,
                    times = countdown,
                    transfers = st.transferIds
                        .filter { it !in selfLineIds }
                        .mapNotNull { id ->
                            lineIdMap[id]?.let { name ->
                                MetroTransfer(name.removePrefix("地铁"), lineColors[id].orEmpty())
                            }
                        }
                        .filter { tf -> tf.name !in selfNames }
                        .distinctBy { it.name },
                    si = st.si,
                    lat = st.lat,
                    lng = st.lng,
                    door = st.door
                )
            )

        }
        return out
    }
}

data class MetroTransfer(val name: String, val color: String)

fun offsetFromServer(serverMillis: Long): Long {
    if (serverMillis <= 0L) return 0L
    val diff = serverMillis - System.currentTimeMillis()
    return if (kotlin.math.abs(diff) > 24L * 3600 * 1000) 0L else diff
}

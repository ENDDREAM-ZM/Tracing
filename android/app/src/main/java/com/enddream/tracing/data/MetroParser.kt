package com.enddream.tracing.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class MetroStationRow(
    val name: String,
    val lng: Double,
    val lat: Double,
    val si: String = "",
    val transferIds: List<String> = emptyList(),
    val door: String = "",
    val poiid: String = ""
)

data class MetroTime(
    val lineId: String,
    val first: String,
    val last: String,
    val toward: String
)

data class MetroLineRow(
    val name: String,
    val fullName: String,
    val color: String,
    val stations: List<MetroStationRow>,
    val loop: Boolean = false
)

object MetroParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): List<MetroLineRow> = runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        val lines = root["l"]?.jsonArray ?: JsonArray(emptyList())
        lines.mapNotNull { element ->
            val line = element as? JsonObject ?: return@mapNotNull null
            val name = line["ln"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val fullName = line["kn"]?.jsonPrimitive?.content ?: name
            val color = line["cl"]?.jsonPrimitive?.content.orEmpty()
            val loop = line["lo"]?.jsonPrimitive?.content == "1"
            val stations = (line["st"] as? JsonArray)?.mapNotNull { se ->
                val st = se as? JsonObject ?: return@mapNotNull null
                val n = st["n"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val sl = st["sl"]?.jsonPrimitive?.content.orEmpty()
                val parts = sl.split(",")
                val lng = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val lat = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                val si = st["si"]?.jsonPrimitive?.content.orEmpty()
                val udli = st["r"]?.jsonPrimitive?.content.orEmpty()
                val ids = udli.split("|").map { it.trim() }.filter { it.isNotBlank() }.distinct()
                val door = st["door"]?.jsonPrimitive?.content
                    ?: st["dr"]?.jsonPrimitive?.content
                    ?: ""
                val poiid = st["poiid"]?.jsonPrimitive?.content.orEmpty()
                if (st["su"]?.jsonPrimitive?.content == "3") null
                else MetroStationRow(n, lng, lat, si, ids, door, poiid)
            } ?: emptyList()
            MetroLineRow(name, fullName, color, stations, loop)
        }
    }.getOrDefault(emptyList())

    fun lineIdMap(raw: String): Map<String, String> = runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        val out = HashMap<String, String>()
        root["l"]?.jsonArray?.forEach { e ->
            val line = e as? JsonObject ?: return@forEach
            val full = line["kn"]?.jsonPrimitive?.content
                ?: line["ln"]?.jsonPrimitive?.content
                ?: return@forEach
            line["li"]?.jsonPrimitive?.content.orEmpty().split("|").forEach { id ->
                val t = id.trim()
                if (t.isNotBlank()) out[t] = full
            }
        }
        out
    }.getOrDefault(emptyMap())

    fun parseTimes(raw: String): Map<String, List<MetroTime>> = runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        val out = HashMap<String, MutableList<MetroTime>>()
        root["l"]?.jsonArray?.forEach { e ->
            val line = e as? JsonObject ?: return@forEach
            line["st"]?.jsonArray?.forEach { se ->
                val st = se as? JsonObject ?: return@forEach
                val si = st["si"]?.jsonPrimitive?.content ?: return@forEach
                val list = out.getOrPut(si) { mutableListOf() }
                st["d"]?.jsonArray?.forEach { de ->
                    val d = de as? JsonObject ?: return@forEach
                    val ft = d["ft"]?.jsonPrimitive?.content.orEmpty()
                    val lt = d["lt"]?.jsonPrimitive?.content.orEmpty()
                    if (ft.startsWith("--") && lt.startsWith("--")) return@forEach
                    list.add(
                        MetroTime(
                            lineId = d["ls"]?.jsonPrimitive?.content.orEmpty(),
                            first = ft,
                            last = lt,
                            toward = d["n"]?.jsonPrimitive?.content.orEmpty()
                        )
                    )
                }
            }
        }
        out
    }.getOrDefault(emptyMap())
}

object MetroText {

    private val fareOnly = Regex("^全网?分段.*元。?$")
    private val tableRow = Regex("^(.+?)[：:]【([^】]+)】｛([^｝]+)｝")
    private val direction = Regex("[【｛]往([^：:，】｝]+)[：:]")
    private val clockText = Regex("^\\d{1,2}:\\d{2}$")

    fun announcement(raw: String): String {
        val text = raw.replace("\r", "").trim()
        if (text.isBlank()) return ""
        val kept = ArrayList<String>()
        for (line in text.split("\n")) {
            val t = line.trim()
            if (t.contains("各站首末班") || t.contains("普通列车") || t.contains("时刻表")) break
            if (t.startsWith("★")) continue
            if (tableRow.containsMatchIn(t)) break
            if (fareOnly.matches(t)) continue
            if (t.endsWith("如下：") || t.endsWith("如下:")) continue
            kept.add(line)
        }
        return dedupe(kept.joinToString("\n").trim())
    }

    private val statusLine = listOf("不在运营时间内", "运营时间外", "已停运", "非运营时间")

    fun clean(raw: String): String = raw.replace("\r", "")
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() && statusLine.none { s -> it.startsWith(s) } }
        .joinToString("\n")

    fun dedupe(raw: String, names: List<String> = emptyList()): String {
        val lines = raw.replace("\r", "").split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return lines.firstOrNull().orEmpty()
        val out = ArrayList<String>()
        for (line in lines) {
            val k = key(line, names)
            if (k.isEmpty()) {
                out.add(line)
                continue
            }
            val same = out.indexOfFirst { key(it, names) == k }
            if (same >= 0) {
                if (line.length > out[same].length) out[same] = line
                continue
            }
            val inner = out.indexOfFirst {
                val a = key(it, names)
                a.isNotEmpty() && (a.contains(k) || k.contains(a))
            }
            if (inner < 0) out.add(line) else if (line.length > out[inner].length) out[inner] = line
        }
        return out.joinToString("\n")
    }

    private fun key(text: String, names: List<String> = emptyList()): String {
        var t = text
        names.filter { it.isNotBlank() }.sortedByDescending { it.length }.forEach { t = t.replace(it, "") }
        t = t
            .replace(Regex("地铁[0-9A-Za-z一二三四五六七八九十]+号线"), "")
            .replace(Regex("[0-9A-Za-z一二三四五六七八九十]+号线"), "")
            .replace("地铁", "")
        return t.filter { it.isLetterOrDigit() }
    }

    private fun clock(raw: String): String {
        val t = raw.trim().removeSuffix("*").trim()
        if (clockText.matches(t)) return t
        val core = t.removePrefix("次日").trim()
        return if (t.startsWith("次日") && clockText.matches(core)) "次日$core" else ""
    }

    data class TableRow(
        val name: String,
        val firstToward: Pair<String, String>,
        val secondToward: Pair<String, String>
    )

    data class StationTable(
        val directions: List<String>,
        val rows: List<TableRow>
    )

    fun stationTable(raw: String): StationTable? {
        val text = raw.replace("\r", "")
        val dirs = direction.findAll(text).map { it.groupValues[1].trim() }.take(2).toList()
        if (dirs.size < 2) return null
        val rows = ArrayList<TableRow>()
        for (line in text.split("\n")) {
            val m = tableRow.find(line.trim()) ?: continue
            val a = m.groupValues[2].split("，")
            val b = m.groupValues[3].split("，")
            if (a.size < 2 || b.size < 2) continue
            val name = m.groupValues[1].trim()
            if (name.isBlank()) continue
            rows.add(
                TableRow(
                    name,
                    clock(a[0]) to clock(a[1]),
                    clock(b[0]) to clock(b[1])
                )
            )
        }
        return if (rows.isEmpty()) null else StationTable(dirs, rows)
    }
}

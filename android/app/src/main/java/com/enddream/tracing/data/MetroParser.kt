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
    val poiid: String = "",
    val building: Boolean = false
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
                val building = st["su"]?.jsonPrimitive?.content == "3"
                MetroStationRow(n, lng, lat, si, ids, door, poiid, building)
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

    fun announcement(raw: String): String {
        val text = raw.replace("\r", "").trim()
        if (text.isBlank()) return ""
        val kept = ArrayList<String>()
        for (line in text.split("\n")) {
            val t = line.trim()
            if (t.startsWith("各站首末班") || t.startsWith("普通列车")) break
            if (fareOnly.matches(t)) continue
            if (t.endsWith("如下：") || t.endsWith("如下:")) continue
            kept.add(line)
        }
        return kept.joinToString("\n").trim()
    }
}

package com.enddream.tracing.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

class MetroApi {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun spellMap(): Map<String, String> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://webapi.amap.com/subway/data/citylist.json")
            .header("User-Agent", UA)
            .build()
        runCatching {
            client.newCall(req).execute().use { r ->
                val root = json.parseToJsonElement(r.body?.string() ?: "{}").jsonObject
                val arr = root["citylist"]?.jsonArray ?: return@use emptyMap()
                arr.mapNotNull { e ->
                    val o = e as? JsonObject ?: return@mapNotNull null
                    val code = o["adcode"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val spell = o["spell"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    code to spell
                }.toMap()
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun lineNotice(
        lat1: Double,
        lng1: Double,
        poi1: String,
        lat2: Double,
        lng2: Double,
        poi2: String
    ): String = withContext(Dispatchers.IO) {
        if (lat1 == 0.0 || lng1 == 0.0 || lat2 == 0.0 || lng2 == 0.0) return@withContext ""
        val url = "https://webapi.amap.com/subway/service/navigation/busExt" +
            "?x1=$lng1&y1=$lat1&x2=$lng2&y2=$lat2" +
            "&poiid1=$poi1&poiid2=$poi2&uuid=${UUID.randomUUID().toString().replace("-", "")}" +
            "&type=6&Ver=3"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://m.amap.com/subway/")
            .build()
        runCatching {
            client.newCall(req).execute().use { r ->
                val root = json.parseToJsonElement(r.body?.string() ?: "{}").jsonObject
                val bus = root["buslist"]?.jsonArray?.firstOrNull() as? JsonObject
                    ?: return@use ""
                val seg = bus["segmentlist"]?.jsonArray?.firstOrNull() as? JsonObject
                val parts = listOfNotNull(
                    seg?.get("bus_des")?.jsonPrimitive?.contentOrNull,
                    bus["risk_time_des"]?.jsonPrimitive?.contentOrNull,
                    bus["time_tag_des"]?.jsonPrimitive?.contentOrNull
                ).map { it.trim() }.filter { it.isNotBlank() }
                parts.joinToString("\n")
            }
        }.getOrDefault("")
    }

    suspend fun lineExt(
        lat1: Double,
        lng1: Double,
        poi1: String,
        lat2: Double,
        lng2: Double,
        poi2: String
    ): Pair<Int, Double>? = withContext(Dispatchers.IO) {
        if (lat1 == 0.0 || lng1 == 0.0 || lat2 == 0.0 || lng2 == 0.0) return@withContext null
        val url = "https://webapi.amap.com/subway/service/navigation/busExt" +
            "?x1=$lng1&y1=$lat1&x2=$lng2&y2=$lat2" +
            "&poiid1=$poi1&poiid2=$poi2&uuid=${UUID.randomUUID().toString().replace("-", "")}" +
            "&type=6&Ver=3"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://m.amap.com/subway/")
            .build()
        runCatching {
            client.newCall(req).execute().use { r ->
                val root = json.parseToJsonElement(r.body?.string() ?: "{}").jsonObject
                val bus = root["buslist"]?.jsonArray?.firstOrNull() as? JsonObject
                    ?: return@use null
                val length = bus["allLength"]?.jsonPrimitive?.intOrNull ?: 0
                val seg = bus["segmentlist"]?.jsonArray?.firstOrNull() as? JsonObject
                val price = seg?.get("price")?.jsonPrimitive?.doubleOrNull
                    ?: bus["expense"]?.jsonPrimitive?.doubleOrNull
                    ?: 0.0
                if (length <= 0 && price <= 0.0) null else length to price
            }
        }.getOrNull()
    }

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

package com.enddream.tracing.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
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

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

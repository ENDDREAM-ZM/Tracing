package com.enddream.tracing.data

import com.enddream.tracing.data.api.BusApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetTime {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun serverMillis(): Long = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://h5.mygolbs.com/ApiData.do")
                .head()
                .header("User-Agent", BusApi.UA)
                .build()
            client.newCall(req).execute().use { r ->
                r.header("Date")?.let { parseHttpDate(it) } ?: 0L
            }
        }.getOrDefault(0L)
    }

    private fun parseHttpDate(text: String): Long = runCatching {
        val fmt = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
        fmt.parse(text)?.time ?: 0L
    }.getOrDefault(0L)
}

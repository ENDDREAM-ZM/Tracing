package com.enddream.tracing.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class BusApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val baseUrl = "https://h5.mygolbs.com/ApiData.do"

    private fun form(vararg pairs: Pair<String, String>): FormBody =
        FormBody.Builder().apply {
            pairs.forEach { (k, v) -> add(k, v) }
        }.build()

    private fun request(body: FormBody): Request =
        Request.Builder()
            .url(baseUrl)
            .post(body)
            .header("Referer", "https://h5.mygolbs.com/sdbus.html")
            .header("Origin", "https://h5.mygolbs.com")
            .header("User-Agent", UA)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

    private suspend fun post(body: FormBody): String = withContext(Dispatchers.IO) {
        client.newCall(request(body)).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body?.string() ?: ""
        }
    }

    private inline fun <reified T> decode(raw: String): T =
        AppJson.decodeFromString<T>(raw)

    suspend fun cityList(): List<String> {
        val raw = post(form("CMD" to Cmd.CITY_LIST.toString()))
        return decode<CityListResponse>(raw).data.map { it.name }.filter { it.isNotBlank() }
    }

    suspend fun allLines(cityName: String): List<BusLine> {
        val raw = post(
            form(
                "CMD" to Cmd.SEARCH_LINE_ALL.toString(),
                "CITYNAME" to cityName,
                "KEYWORD" to ""
            )
        )
        val r = runCatching { decode<LineSearchResponse>(raw) }.getOrNull()
        val list = r?.let { if (it.buslines.isNotEmpty()) it.buslines else it.data } ?: emptyList()
        return list.distinctBy { "${it.lineName}|${it.upperOrDown}" }
            .sortedWith(compareBy({ it.lineName.take(1).toIntOrNull() ?: 9999 }, { it.lineName }))
    }

    suspend fun searchLine(cityName: String, keyword: String): List<BusLine> {
        val raw = post(
            form(
                "CMD" to Cmd.SEARCH_LINE.toString(),
                "CITYNAME" to cityName,
                "KEYWORD" to keyword
            )
        )
        val r = runCatching { decode<LineSearchResponse>(raw) }.getOrNull()
        return r?.let { if (it.buslines.isNotEmpty()) it.buslines else it.data } ?: emptyList()
    }

    suspend fun nearby(cityName: String, lng: Double, lat: Double): List<NearbyStation> {
        val raw = post(
            form(
                "CMD" to Cmd.NEARBY.toString(),
                "CITYNAME" to cityName,
                "LNG" to lng.toString(),
                "LAT" to lat.toString()
            )
        )
        return runCatching { decode<NearbyResponse>(raw).data }.getOrDefault(emptyList())
    }

    suspend fun routeDetail(cityName: String, lineName: String, direction: Int): RouteDetail? {
        val raw = post(
            form(
                "CMD" to Cmd.ROUTE_DETAIL.toString(),
                "CITYNAME" to cityName,
                "LINENAME" to lineName,
                "DIRECTION" to direction.toString()
            )
        )
        return runCatching { decode<RouteDetail>(raw) }.getOrNull()?.takeIf { it.routeName.isNotBlank() }
    }

    suspend fun realtime(
        cityName: String,
        lineName: String,
        direction: Int,
        stationOrder: Int
    ): RealtimeResponse {
        val raw = post(
            form(
                "CMD" to Cmd.REALTIME.toString(),
                "CITYNAME" to cityName,
                "LINENAME" to lineName,
                "DIRECTION" to direction.toString(),
                "STATIONORDER" to stationOrder.toString()
            )
        )
        val decoded = runCatching { decode<RealtimeResponse>(raw) }.getOrNull()
            ?: return RealtimeResponse()
        val buses = runCatching {
            val root = AppJson.parseToJsonElement(raw).jsonObject
            AppJson.decodeFromJsonElement<List<RealBus>>(root["list"] ?: JsonArray(emptyList()))
        }.getOrDefault(emptyList())
        return decoded.copy(listBuses = buses)
    }

    suspend fun busEtas(
        cityName: String,
        lineName: String,
        direction: Int,
        targets: Map<String, Int>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val wanted = targets.filter { it.key.isNotBlank() && it.value > 0 }
        if (wanted.isEmpty()) return@withContext emptyMap()
        val out = java.util.concurrent.ConcurrentHashMap<String, String>()
        coroutineScope {
            wanted.entries.take(16).map { (busNumber, order) ->
                async {
                    val raw = runCatching {
                        post(
                            form(
                                "CMD" to Cmd.REALTIME.toString(),
                                "CITYNAME" to cityName,
                                "LINENAME" to lineName,
                                "DIRECTION" to direction.toString(),
                                "STATIONORDER" to order.toString()
                            )
                        )
                    }.getOrNull() ?: return@async
                    val info = runCatching { decode<RealtimeResponse>(raw) }.getOrNull() ?: return@async
                    val tip = info.routeOnStationRTimeInfoList
                        .firstOrNull { it.busNumber == busNumber }
                        ?.busToStationTimeTips?.trim().orEmpty()
                    if (tip.isNotBlank()) out[busNumber] = tip
                }
            }.awaitAll()
        }
        out.toMap()
    }

    suspend fun linesByStation(
        cityName: String,
        stationName: String,
        lng: Double,
        lat: Double
    ): List<BusLine> {
        val raw = post(
            form(
                "CMD" to Cmd.LINE_BY_STATION_V2.toString(),
                "CITYNAME" to cityName,
                "STATIONNAME" to stationName,
                "MYLNG" to lng.toString(),
                "MYLAT" to lat.toString(),
                "ALL" to "1"
            )
        )
        val r = runCatching { decode<LineSearchResponse>(raw) }.getOrNull()
        return r?.let { if (it.buslines.isNotEmpty()) it.buslines else it.data } ?: emptyList()
    }

    suspend fun metroCities(): List<MetroCity> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://metro.mygolbs.com/MetroCitys.json")
            .header("User-Agent", UA)
            .build()
        runCatching {
            client.newCall(req).execute().use { r ->
                decode<List<MetroCity>>(r.body?.string() ?: "[]")
            }
        }.getOrDefault(emptyList())
    }

    suspend fun metroSpellMap(): Map<String, String> = MetroApi().spellMap()


    suspend fun depart(cityName: String, routeId: Int, direction: Int): List<DepartGroup> {
        if (routeId <= 0) return emptyList()
        val raw = post(
            form(
                "CMD" to "207",
                "CITYNAME" to cityName,
                "DIRECTION" to direction.toString(),
                "ROUTEID" to routeId.toString(),
                "CITYKEY" to ""
            )
        )
        return runCatching { decode<DepartResponse>(raw) }.getOrNull()?.list.orEmpty()
    }

    suspend fun metroInfo(adcode: String, citySpell: String): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://webapi.amap.com/subway/data/${adcode}_info_${citySpell}.json")
            .header("User-Agent", UA)
            .header("Referer", "https://map.amap.com/subway/index.html")
            .build()
        runCatching {
            client.newCall(req).execute().use { r ->
                if (r.isSuccessful) r.body?.string() else null
            }
        }.getOrNull()
    }

    suspend fun metroNetwork(adcode: String, citySpell: String): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://webapi.amap.com/subway/data/${adcode}_drw_${citySpell}.json")
            .header("User-Agent", UA)
            .build()
        runCatching {
            client.newCall(req).execute().use { r ->
                if (r.isSuccessful) r.body?.string() else null
            }
        }.getOrNull()
    }

    companion object {
        const val UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

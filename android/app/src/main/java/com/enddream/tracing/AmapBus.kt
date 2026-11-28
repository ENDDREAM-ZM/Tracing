package com.enddream.tracing

import android.content.Context
import com.amap.api.services.busline.BusLineItem
import com.amap.api.services.busline.BusLineQuery
import com.amap.api.services.busline.BusLineSearch
import com.amap.api.services.core.ServiceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object AmapBus {

    data class Line(
        val name: String,
        val from: String,
        val to: String,
        val first: String,
        val last: String,
        val price: String,
        val company: String,
        val stationCount: Int
    )

    @Volatile
    private var ready = false

    fun init(ctx: Context) {
        if (ready) return
        ready = true
        runCatching {
            val app = ctx.applicationContext
            ServiceSettings.updatePrivacyShow(app, true, true)
            ServiceSettings.updatePrivacyAgree(app, true)
            ServiceSettings.getInstance().setProtocol(ServiceSettings.HTTPS)
        }
    }

    private fun timeOf(d: java.util.Date?): String {
        if (d == null) return ""
        val f = java.text.SimpleDateFormat("HH:mm", java.util.Locale.CHINA)
        return f.format(d)
    }

    suspend fun search(
        ctx: Context,
        city: String,
        keyword: String,
        timeoutMs: Long = 8000L
    ): List<Line> = withContext(Dispatchers.IO) {
        init(ctx)
        withTimeoutOrNull(timeoutMs) {
            runCatching {
                val q = BusLineQuery(
                    keyword,
                    BusLineQuery.SearchType.BY_LINE_NAME,
                    city
                ).apply {
                    setPageSize(20)
                    setExtensions(BusLineSearch.EXTENSIONS_ALL)
                }
                val items: List<BusLineItem> = BusLineSearch(ctx.applicationContext, q)
                    .searchBusLine()
                    ?.busLines
                    .orEmpty()
                items.map { it.toLine() }
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    private fun BusLineItem.toLine(): Line {
        val priceText = when {
            totalPrice > 0f -> trimPrice(totalPrice)
            basicPrice > 0f -> trimPrice(basicPrice)
            else -> ""
        }
        return Line(
            name = busLineName.orEmpty(),
            from = originatingStation.orEmpty(),
            to = terminalStation.orEmpty(),
            first = timeOf(firstBusTime),
            last = timeOf(lastBusTime),
            price = priceText,
            company = busCompany.orEmpty(),
            stationCount = busStations?.size ?: 0
        )
    }

    private fun trimPrice(v: Float): String {
        val n = v.toDouble()
        return if (n == n.toInt().toDouble()) "${n.toInt()}元" else String.format(
            java.util.Locale.CHINA,
            "%.1f元",
            n
        )
    }

    fun matches(line: Line, from: String, to: String): Boolean {
        val f = from.trim()
        val t = to.trim()
        if (f.isBlank() || t.isBlank()) return true
        val a = line.from.trim()
        val b = line.to.trim()
        return (a == f && b == t) || (a == t && b == f)
    }
}

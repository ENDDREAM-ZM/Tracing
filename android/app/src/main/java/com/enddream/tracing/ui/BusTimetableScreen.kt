package com.enddream.tracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.enddream.tracing.AmapBus
import com.enddream.tracing.data.RouteCache
import com.enddream.tracing.data.api.BusApi
import com.enddream.tracing.data.api.DepartGroup
import com.enddream.tracing.data.api.RouteDetail

@Composable
fun BusTimetableScreen(
    city: String,
    line: String,
    direction: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var detail by remember(line, direction) { mutableStateOf<RouteDetail?>(null) }
    var groups by remember(line, direction) { mutableStateOf<List<DepartGroup>>(emptyList()) }
    var loading by remember(line, direction) { mutableStateOf(true) }
    var amapLine by remember(line, direction) { mutableStateOf<AmapBus.Line?>(null) }
    var amapTried by remember(line, direction) { mutableStateOf(false) }

    LaunchedEffect(city, line, direction) {
        loading = true
        val api = BusApi()
        val d = runCatching { RouteCache.get(api, city, line, direction) }.getOrNull()
        detail = d
        val list = if (d != null) {
            runCatching { api.depart(city, d.routeId, direction) }.getOrDefault(emptyList())
        } else emptyList()
        groups = list
        loading = false
        val from = d?.data?.firstOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()
        val to = d?.data?.lastOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()
        val hits = runCatching { AmapBus.search(ctx, city, line) }.getOrDefault(emptyList())
        amapLine = hits.firstOrNull { AmapBus.matches(it, from, to) } ?: hits.firstOrNull()
        amapTried = true
    }

    val start = detail?.data?.firstOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()
    val end = detail?.data?.lastOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()
    val first = detail?.beginTime.orEmpty()
        .ifBlank { detail?.firstLast?.firstOrNull()?.first.orEmpty() }
    val last = detail?.endTime.orEmpty()
        .ifBlank { detail?.firstLast?.firstOrNull()?.last.orEmpty() }
    val nowMinute = remember { java.util.Calendar.getInstance().let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) } }

    Box(Modifier.fillMaxSize()) {
        GlassFrosted(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) {}

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        displayLine(line),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        "$start → $end",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            GlassPanel(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeBadge(true, first.ifBlank { "—" }, 11)
                        Spacer(Modifier.width(10.dp))
                        TimeBadge(false, last.ifBlank { "—" }, 11)
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (detail?.planTime.orEmpty().isNotBlank())
                                "起点预计发车 ${detail?.planTime}"
                            else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            amapLine?.let { a ->
                Spacer(Modifier.height(10.dp))
                GlassPanel(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "高德公交信息",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TimeBadge(true, a.first.ifBlank { "—" }, 11)
                            Spacer(Modifier.width(10.dp))
                            TimeBadge(false, a.last.ifBlank { "—" }, 11)
                            Spacer(Modifier.weight(1f))
                            Text(
                                listOfNotNull(
                                    a.price.takeIf { it.isNotBlank() },
                                    "${a.stationCount} 站".takeIf { a.stationCount > 0 }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        if (a.company.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                a.company,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            GlassPanel(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 10.dp)
                    .weight(1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("正在获取时刻表…", color = TextMuted)
                    }

                    groups.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Text(
                                "该线路未提供发车时刻表，下列为全部站点",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                            )
                        }
                        itemsIndexed(detail?.data.orEmpty()) { i, st ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    st.stationName.ifBlank { st.showName },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }

                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Text(
                                "掌上公交发车时刻表",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
                            )
                        }
                        itemsIndexed(groups) { i, g ->
                            DepartGroupRow(g, nowMinute, i == groups.lastIndex)
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartGroupRow(group: DepartGroup, nowMinute: Int, last: Boolean) {
    val times = group.times.filter { it.time.isNotBlank() }
    if (times.isEmpty()) return
    val current = group.isNow == 1
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(
                        if (current) MaterialTheme.colorScheme.primary else TextMuted.copy(alpha = 0.5f),
                        RoundedCornerShape(3.dp)
                    )
            )
            Spacer(Modifier.width(7.dp))
            Text(
                group.group,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 13.sp,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                color = if (current) MaterialTheme.colorScheme.primary else TextPrimary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                when (group.peak) {
                    1 -> "高峰"
                    2 -> "晚高峰"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        FlowTimes(times.map { it.time }, nowMinute)
        if (!last) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowTimes(times: List<String>, nowMinute: Int) {
    val next = times.firstOrNull { clockMinutes(it) >= nowMinute }
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        times.forEach { t ->
            val isNext = t == next
            GlassChip(corner = 7.dp, selected = isNext) {
                Text(
                    t,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    color = if (isNext) MaterialTheme.colorScheme.primary else TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun clockMinutes(text: String): Int {
    val t = text.trim()
    val h = t.substringBefore(":").toIntOrNull() ?: return -1
    val m = t.substringAfter(":", "").take(2).toIntOrNull() ?: return -1
    return h * 60 + m
}

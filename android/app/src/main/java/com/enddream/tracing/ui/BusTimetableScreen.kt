package com.enddream.tracing.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.enddream.tracing.data.MetroTimetable
import com.enddream.tracing.data.RouteCache
import com.enddream.tracing.data.api.BusApi
import com.enddream.tracing.data.api.RouteDetail

@Composable
fun BusTimetableScreen(
    city: String,
    line: String,
    direction: Int,
    onBack: () -> Unit
) {
    var detail by remember(line, direction) { mutableStateOf<RouteDetail?>(null) }

    LaunchedEffect(city, line, direction) {
        detail = runCatching { RouteCache.get(BusApi(), city, line, direction) }.getOrNull()
    }

    val start = detail?.data?.firstOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()
    val end = detail?.data?.lastOrNull()?.let { it.stationName.ifBlank { it.showName } }.orEmpty()
    val first = detail?.beginTime.orEmpty()
        .ifBlank { detail?.firstLast?.firstOrNull()?.first.orEmpty() }
    val last = detail?.endTime.orEmpty()
        .ifBlank { detail?.firstLast?.firstOrNull()?.last.orEmpty() }
    val times = remember(detail) { MetroTimetable.busTimeline(first, last) }

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
                                "计划 ${detail?.planTime}"
                            else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
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
                if (times.isEmpty()) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Text(
                                "该线路未提供首末班数据，下列为全部站点",
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
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Text(
                                "全程发车时刻（按首末班与间隔推算）",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                            )
                        }
                        itemsIndexed(times) { i, t ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    t,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (i == 0) "首班" else if (i == times.lastIndex) "末班" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }
                }
            }
        }
    }
}

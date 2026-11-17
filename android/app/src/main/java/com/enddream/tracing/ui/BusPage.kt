package com.enddream.tracing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enddream.tracing.data.api.BusLine
import com.enddream.tracing.data.api.NearbyStation

@Composable
fun BusPage(
    state: BusUiState,
    city: String,
    onKeyword: (String) -> Unit,
    onOpenLine: (CardLine) -> Unit,
    onOpenStation: (NearbyStation) -> Unit,
    onOpenCityLine: (BusLine) -> Unit,
    onLocate: () -> Unit,
    onPullRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searching = state.keyword.isNotBlank()

    Column(modifier.fillMaxSize()) {
        GlassPill(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .height(46.dp)
        ) {
            SearchField(
                value = state.keyword,
                onValueChange = onKeyword,
                onSearch = {},
                placeholder = if (state.lines.isEmpty()) "搜索线路" else "${state.lines.size} 条线路",
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            when {
                searching && state.loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                searching -> GlassPanel(modifier = Modifier.fillMaxSize()) {
                    if (state.lines.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("没有找到线路", color = TextMuted)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            item { LineTableHeader() }
                            itemsIndexed(
                                state.lines,
                                key = { i, l -> "line-$i-${l.lineName}-${l.upperOrDown}" }
                            ) { _, line ->
                                val dir = line.upperOrDown.toIntOrNull() ?: 1
                                val cl = CardLine(line.lineName, dir, line.from, line.to)
                                TableDivider()
                                LineTableRow(
                                    line = line.lineName,
                                    from = line.from,
                                    current = "",
                                    to = line.to,
                                    onOpen = { onOpenLine(cl) }
                                )
                            }
                        }
                    }
                }

                else -> PullRefresh(refreshing = state.refreshing, onRefresh = onPullRefresh) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item { SectionHeader("附近站点", state.loadingNearby, "定位", onLocate) }
                        if (state.nearbyDetail.isEmpty()) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "点「定位」查看附近站点和经过的线路",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                        itemsIndexed(
                            state.nearbyDetail,
                            key = { i, n -> "nb-$i-${n.station.name}" }
                        ) { _, item ->
                            LineTableCard(
                                title = item.station.name,
                                subtitle = formatDistance(item.station.dis),
                                loading = item.loading,
                                lines = item.lines,
                                firstLast = item.firstLast,
                                onOpenStation = { onOpenStation(item.station) },
                                onOpenLine = onOpenLine,
                                onFav = {},
                                onGo = { onOpenStation(item.station) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        item { SectionHeader("$city 其他线路", false, "", {}) }
                        item {
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                if (state.lines.isEmpty()) {
                                    Text(
                                        "暂无线路",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                } else {
                                    Column {
                                        LineTableHeader(showCurrentCol = false)
                                        state.lines.take(80).forEach { line ->
                                            TableDivider()
                                            LineTableRow(
                                                line = line.lineName,
                                                from = line.from,
                                                current = "",
                                                to = line.to,
                                                showCurrentCol = false,
                                                onOpen = { onOpenCityLine(line) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    loading: Boolean,
    action: String,
    onAction: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (loading) {
            Spacer(Modifier.size(8.dp))
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(12.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.weight(1f))
        if (action.isNotBlank()) {
            Text(
                if (loading) "定位中…" else action,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun formatDistance(m: Int): String =
    if (m >= 1000) String.format("%.1fkm", m / 1000f) else "${m}m"

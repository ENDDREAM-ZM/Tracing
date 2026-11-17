package com.enddream.tracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enddream.tracing.Notify
import com.enddream.tracing.data.Prefs
import com.enddream.tracing.data.RouteRows
import kotlinx.coroutines.launch

@Composable
fun RouteScreen(
    city: String,
    line: String,
    direction: Int,
    from: String,
    to: String,
    refreshSec: Int,
    notifyOn: Boolean,
    highlightStation: String?,
    onNavigate: (Double, Double, String) -> Unit,
    onBack: () -> Unit,
    vm: RouteViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFav by remember { mutableStateOf(false) }
    var dir by remember { mutableIntStateOf(direction) }
    var alertStation by remember { mutableStateOf<String?>(null) }
    var alerted by remember { mutableStateOf(false) }
    var navStation by remember { mutableStateOf<com.enddream.tracing.data.RailRow.Station?>(null) }
    var picked by remember(city, line, dir) { mutableStateOf(highlightStation.orEmpty()) }
    var showTimetable by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val headTo = RouteRows.lastStation(state.detail).ifBlank {
        if (dir == 1) to else from
    }
    val stationNames = state.detail?.data.orEmpty()
        .map { it.stationName.ifBlank { it.showName } }
    val favFrom = stationNames.firstOrNull().orEmpty()
    val favTo = RouteRows.lastStation(state.detail).ifBlank { to }

    LaunchedEffect(city, line, dir) {
        isFav = Prefs.isFavorite(ctx, city, line, dir, "bus")
        vm.load(city, line, dir)
        vm.loadRealtime(city, line, dir)
    }

    LaunchedEffect(city, line, dir, refreshSec) {
        while (true) {
            kotlinx.coroutines.delay(refreshSec * 1000L)
            vm.loadRealtime(city, line, dir)
        }
    }

    LaunchedEffect(highlightStation, state.detail, state.buses) {
        val target = picked.ifBlank { highlightStation } ?: return@LaunchedEffect
        if (state.detail == null) return@LaunchedEffect
        val rows = RouteRows.build(state.detail, state.buses, state.speeds)
        val idx = rows.indexOfFirst {
            it is com.enddream.tracing.data.RailRow.Station && it.name == target
        }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    LaunchedEffect(state.arrived, state.upcoming, alertStation, notifyOn) {
        val target = alertStation ?: return@LaunchedEffect
        if (!notifyOn) return@LaunchedEffect
        if (alerted) return@LaunchedEffect
        if (target in state.arrived || target in state.upcoming) {
            alerted = true
            Notify.fire(
                ctx,
                ("$line|$target").hashCode(),
                "${displayLine(line)} 即将到站",
                "$target 车辆即将到站，请准备下车"
            )
        }
    }

    if (showTimetable) {
        BusTimetableScreen(
            city = city,
            line = line,
            direction = dir,
            onBack = { showTimetable = false }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "线路详情",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Box(Modifier.width(48.dp))
            }

            GlassPanel(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayLine(state.detail?.routeName ?: line),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "往 $headTo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        FavButton(
                            fav = isFav,
                            showLabel = true,
                            onClick = {
                                scope.launch {
                                    Prefs.toggleFavorite(
                                        ctx,
                                        com.enddream.tracing.data.Favorite(
                                            city, line, dir, favFrom, favTo, "bus"
                                        )
                                    )
                                    isFav = Prefs.isFavorite(ctx, city, line, dir, "bus")
                                }
                            }
                        )
                    }
                    val d0 = state.detail
                    if (d0 != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (d0.beginTime.isNotBlank()) TimeBadge(true, d0.beginTime, 10)
                            if (d0.endTime.isNotBlank()) TimeBadge(false, d0.endTime, 10)
                            Text(
                                "共 ${d0.data.size} 站",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Spacer(Modifier.weight(1f))
                        }
                        if (d0.commonts.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                d0.commonts,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeedLegend()
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clickable {
                            dir = if (dir == 1) 2 else 1
                            alerted = false
                        }
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "换向",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .clickable { showTimetable = true }
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "时刻表",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            when {
                state.loading && state.detail == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                state.error != null && state.detail == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.error ?: "加载失败", color = TextMuted)
                }

                else -> {
                    val d = state.detail
                    if (d != null) {
                        GlassPanel(
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 10.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            val rows = remember(d, state.buses, state.speeds) {
                                RouteRows.build(d, state.buses, state.speeds)
                            }
                            RailList(
                                rows = rows,
                                alertStation = alertStation,
                                onAlert = { name ->
                                    alertStation = if (alertStation == name) null else name
                                    alerted = false
                                },
                                modifier = Modifier.padding(vertical = 6.dp),
                                alertLabel = "到站提醒",
                                highlightStation = picked.ifBlank { highlightStation },
                                listState = listState,
                                onStationClick = { st ->
                                    picked = st.name
                                    navStation = st
                                }
                            )
                        }
                    }
                }
            }
        }

        navStation?.let { st ->
            AlertDialog(
                onDismissRequest = { navStation = null },
                confirmButton = {
                    TextButton(onClick = {
                        navStation = null
                        onNavigate(st.lat, st.lng, "${st.name}站")
                    }) { Text("导航") }
                },
                dismissButton = {
                    TextButton(onClick = { navStation = null }) { Text("取消") }
                },
                title = { Text(st.name) },
                text = { Text("是否导航到「${st.name}」站？") }
            )
        }
    }
}

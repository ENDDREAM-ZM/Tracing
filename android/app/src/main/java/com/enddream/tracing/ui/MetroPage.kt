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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enddream.tracing.Notify
import com.enddream.tracing.data.Prefs
import com.enddream.tracing.data.MetroPredict
import com.enddream.tracing.data.MetroTimetable
import com.enddream.tracing.data.api.MetroCity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun MetroPage(
    city: String,
    notifyOn: Boolean,
    onAskNotify: () -> Unit = {},
    onNavigate: (name: String, lat: Double, lng: Double) -> Unit,
    vm: MetroViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var alertStation by remember { mutableStateOf<String?>(null) }
    val alertKey = state.selectedLine?.fullName.orEmpty() + "|" + if (state.reverse) 2 else 1
    LaunchedEffect(alertKey) {
        alertStation = com.enddream.tracing.data.AlertStore.get(
            state.selectedLine?.fullName.orEmpty(),
            if (state.reverse) 2 else 1
        )
    }
    var alerted by remember { mutableStateOf(false) }
    var detailStation by remember { mutableStateOf<com.enddream.tracing.data.RailRow.Station?>(null) }
    var showTimetable by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(city) { vm.loadCities(city, auto = true) }

    LaunchedEffect(Unit) {
        if (Prefs.metroNoticeOff(ctx)) vm.noticeOff = true
    }

    LaunchedEffect(state.selectedLine, state.reverse) {
        val line = state.selectedLine ?: return@LaunchedEffect
        vm.loadNotice(line.name, if (state.reverse) 2 else 1)
    }

    LaunchedEffect(state.selectedLine, alertStation, state.minuteOfDay) {
        val target = alertStation ?: return@LaunchedEffect
        if (alerted) return@LaunchedEffect
        val line = state.selectedLine ?: return@LaunchedEffect
        val st = line.stations.firstOrNull { it.name == target } ?: return@LaunchedEffect
        val selfIds = lineIdsOf(state.lineIdMap, line)
        val entry = state.times[st.si].orEmpty()
            .filter { selfIds.isEmpty() || it.lineId in selfIds }
        val peak = MetroTimetable.isPeak(state.minuteOfDay)
        val soon = entry.any { t ->
            MetroTimetable.twoArrivals(t.first, t.last, state.minuteOfDay, peak)
                .firstOrNull() == "即将到达"
        }
        if (soon) {
            alerted = true
            alertStation = null
            com.enddream.tracing.data.AlertStore.set(
                line.fullName,
                if (state.reverse) 2 else 1,
                null
            )
            Notify.fire(
                ctx,
                ("metro|${line.fullName}|$target").hashCode(),
                "${displayMetroLine(line.fullName)} 到站提醒",
                "车辆即将到达 $target 站，请准备上下车"
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        state.notice?.let { n ->
            Spacer(Modifier.height(2.dp))
            NoticeBanner(
                notice = n,
                onDismiss = { forever ->
                    vm.dismissNotice(forever)
                    if (forever) scope.launch { Prefs.setMetroNoticeOff(ctx, true) }
                }
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.weight(1f)) {
            val line = state.selectedLine
            val mc = state.city

            when {
                state.loading && mc == null -> CenteredProgress()
                line != null -> Column(Modifier.fillMaxSize()) {
                    val fl = vm.lineFirstLast(line)
                    val cityName = state.city?.cityname.orEmpty()
                    var metroFav by remember(cityName, line.fullName, state.reverse) {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(cityName, line.fullName, state.reverse) {
                        metroFav = Prefs.isFavorite(
                            ctx, cityName, line.fullName,
                            if (state.reverse) 2 else 1, "metro"
                        )
                    }
                    SubHeader(
                        title = line.fullName,
                        subtitle = "往 ${if (state.reverse) line.stations.firstOrNull()?.name else line.stations.lastOrNull()?.name} 方向",
                        firstLast = fl,
                        reverse = state.reverse,
                        onReverse = { vm.toggleDirection() },
                        onTimetable = { showTimetable = true },
                        isFav = metroFav,
                        onFav = {
                            scope.launch {
                                val fromName = if (state.reverse) line.stations.lastOrNull()?.name.orEmpty()
                                else line.stations.firstOrNull()?.name.orEmpty()
                                val toName = if (state.reverse) line.stations.firstOrNull()?.name.orEmpty()
                                else line.stations.lastOrNull()?.name.orEmpty()
                                val added = Prefs.toggleFavorite(
                                    ctx,
                                    com.enddream.tracing.data.Favorite(
                                        cityName,
                                        line.fullName,
                                        if (state.reverse) 2 else 1,
                                        fromName,
                                        toName,
                                        "metro"
                                    )
                                )
                                metroFav = added
                                android.widget.Toast.makeText(
                                    ctx,
                                    if (added) "已收藏该方向" else "已取消收藏",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) { vm.backToLines() }
                    val meta = buildString {
                        if (state.lineLength > 0) {
                            append("全程 ")
                            append(String.format("%.1f", state.lineLength / 1000.0))
                            append(" 公里")
                        }
                        val hi = state.linePrice
                        val lo = state.linePriceMin
                        if (hi > 0.0) {
                            if (isNotBlank()) append("     ")
                            append("票价 ")
                            append(
                                if (lo > 0.0 && lo < hi) "${fmtYuan(lo)}-${fmtYuan(hi)} 元"
                                else "${fmtYuan(hi)} 元"
                            )
                        }
                    }
                    if (meta.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                    state.noticeText?.takeIf { it.isNotBlank() }?.let { txt ->
                        Spacer(Modifier.height(8.dp))
                        NoticeCard(txt)
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassPanel(modifier = Modifier.weight(1f)) {
                        val rows = remember(line, state.minuteOfDay, state.reverse, state.times, state.lineIdMap) {
                            MetroPredict.rows(
                                stations = line.stations,
                                minuteOfDay = state.minuteOfDay,
                                color = line.color,
                                times = state.times,
                                lineIdMap = state.lineIdMap,
                                selfLineIds = lineIdsOf(state.lineIdMap, line),
                                reverse = state.reverse,
                                lineColors = state.lineIdMap.entries.associate { e ->
                                    e.key to (state.lines.firstOrNull {
                                        it.fullName == e.value || it.name == e.value
                                    }?.color.orEmpty())
                                }
                            )
                        }
                        PullRefresh(
                            refreshing = state.loading,
                            onRefresh = { vm.reloadDetail() }
                        ) {
                        RailList(
                            rows = rows,
                            alertStation = alertStation,
                            onAlert = { name ->
                                alertStation = if (alertStation == name) null else name
                                com.enddream.tracing.data.AlertStore.set(
                                    state.selectedLine?.fullName.orEmpty(),
                                    if (state.reverse) 2 else 1,
                                    alertStation
                                )
                                alerted = false
                                onAskNotify()
                            },
                            modifier = Modifier.padding(vertical = 6.dp),
                            alertable = true,
                            alertLabel = "到站提醒",
                            lineColor = line.color.toComposeColor(),
                            onStationClick = { st -> detailStation = st }
                        )
                        }
                    }
                }

                mc != null -> Column(Modifier.fillMaxSize()) {
                    SubHeader(
                        title = mc.cityname,
                        subtitle = "${state.lines.size} 条线路",
                        firstLast = null,
                        reverse = false,
                        onReverse = null,
                        onTimetable = null,
                        onBack = { vm.backToCities() }
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassPanel(modifier = Modifier.weight(1f)) {
                        if (state.lines.isEmpty()) {
                            CenteredText(state.error ?: "该城市暂无地铁数据")
                        } else {
                            LazyColumn(Modifier.fillMaxSize()) {
                                itemsIndexed(state.lines, key = { i, r -> "ml-$i-${r.fullName}" }) { _, row ->
                                    val fl = vm.lineFirstLast(row)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { vm.selectLine(row) }
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = row.color.toComposeColor(),
                                            modifier = Modifier.size(11.dp)
                                        ) {}
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                row.fullName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (fl != null) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    TimeBadge(true, fl.first, 9)
                                                    TimeBadge(false, fl.second, 9)
                                                }
                                            } else {
                                                Text(
                                                    "往 ${row.stations.lastOrNull()?.name.orEmpty()} 方向",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                        Text(
                                            "›",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextMuted.copy(alpha = 0.7f)
                                        )
                                    }
                                    RowDivider()
                                }
                            }
                        }
                    }
                }

                else -> GlassPanel(modifier = Modifier.fillMaxSize()) {
                    if (state.cities.isEmpty()) {
                        CenteredText("暂无地铁城市")
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            itemsIndexed(state.cities, key = { i, c2 -> "mc-$i-${c2.citycode}" }) { _, c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.selectCity(c) }
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        c.cityname,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "›",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextMuted.copy(alpha = 0.7f)
                                    )
                                }
                                RowDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimetable) {
        val line = state.selectedLine
        if (line != null) {
            MetroTimetableScreen(
                line = line,
                times = state.times,
                official = state.official,
                selfLineIds = lineIdsOf(state.lineIdMap, line),
                minuteOfDay = state.minuteOfDay,
                initialReverse = state.reverse,
                onBack = { showTimetable = false }
            )
            return
        }
    }

    detailStation?.let { st ->
        val entry = state.times[st.si].orEmpty()
        val firstLast = com.enddream.tracing.data.MetroTimetable.firstLastOf(entry)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { detailStation = null },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val target = state.selectedLine?.stations?.firstOrNull { it.name == st.name }
                    if (target != null) {
                        onNavigate(target.name, target.lat, target.lng)
                    } else if (st.lat != 0.0 && st.lng != 0.0) {
                        onNavigate(st.name, st.lat, st.lng)
                    }
                    detailStation = null
                }) {
                    androidx.compose.material3.Text("导航")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { detailStation = null }) {
                    androidx.compose.material3.Text("取消")
                }
            },
            title = { androidx.compose.material3.Text(st.name) },
            text = {
                Column {
                    Text(
                        "是否导航到「${st.name}」站？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    if (st.transfers.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            st.transfers.forEach { tf ->
                                Box(
                                    Modifier
                                        .background(
                                            tf.color.toComposeColor(),
                                            RoundedCornerShape(5.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        tf.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    if (firstLast != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TimeBadge(true, firstLast.first, 10)
                            TimeBadge(false, firstLast.second, 10)
                        }
                    }
                }
            }
        )
    }
}

internal fun lineIdOf(idMap: Map<String, String>, line: com.enddream.tracing.data.MetroLineRow): String =
    idMap.entries.firstOrNull { it.value == line.fullName }?.key ?: ""

internal fun lineIdsOf(idMap: Map<String, String>, line: com.enddream.tracing.data.MetroLineRow): Set<String> =
    idMap.entries.filter { it.value == line.fullName }.map { it.key }.toSet()

private fun clockToMin(text: String): Int {
    val h = text.substringBefore(":").toIntOrNull() ?: return -999
    val m = text.substringAfter(":").toIntOrNull() ?: return -999
    return h * 60 + m
}

@Composable
private fun NoticeBanner(
    notice: MetroNotice,
    onDismiss: (Boolean) -> Unit
) {
    var askClose by remember { mutableStateOf(false) }
    var forever by remember { mutableStateOf(false) }
    if (askClose) {
        AlertDialog(
            onDismissRequest = { askClose = false },
            confirmButton = {
                TextButton(onClick = {
                    askClose = false
                    onDismiss(forever)
                }) { Text("关闭") }
            },
            dismissButton = {
                TextButton(onClick = { askClose = false }) { Text("取消") }
            },
            title = { Text("关闭提示") },
            text = {
                Column {
                    Text(
                        "关闭后不再提示「${notice.city} 暂无地铁」",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = forever, onCheckedChange = { forever = it })
                        Text(
                            "永久关闭",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        )
    }
    GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${notice.city} 暂无地铁",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "已切换为距离你最近的 ${notice.shown}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "关闭",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier
                    .clickable { askClose = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun CenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(30.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

@Composable
private fun SubHeader(
    title: String,
    subtitle: String,
    firstLast: Pair<String, String>?,
    reverse: Boolean,
    onReverse: (() -> Unit)?,
    onTimetable: (() -> Unit)?,
    isFav: Boolean = false,
    onFav: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(20.dp),
                tint = TextPrimary
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                displayMetroLine(title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        if (firstLast != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AlignedTime(true, firstLast.first)
                AlignedTime(false, firstLast.second)
            }
            Spacer(Modifier.width(8.dp))
        }
        if (onFav != null) {
            FavButton(fav = isFav, showLabel = true, onClick = onFav)
            Spacer(Modifier.width(2.dp))
        }
        if (onTimetable != null || onReverse != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onReverse != null) {
                    Pill(if (reverse) "正向" else "换向", onReverse)
                }
                if (onTimetable != null) {
                    Pill("时刻表", onTimetable)
                }
            }
        }
    }
}

internal fun displayMetroLine(name: String): String =
    name.trim().removePrefix("地铁")

@Composable
private fun AlignedTime(first: Boolean, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(16.dp)
                .background(if (first) FirstColor else LastColor, RoundedCornerShape(4.dp))
                .padding(vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (first) "首" else "末",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = Color.White
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clickable(onClick = onClick)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun String.toComposeColor(): Color = runCatching {
    if (isBlank()) Color(0xFF4FD6C0) else Color(("FF" + this).toLong(16))
}.getOrDefault(Color(0xFF4FD6C0))

private fun fmtYuan(v: Double): String =
    if (kotlin.math.abs(v - v.toInt()) < 0.05) v.toInt().toString() else String.format("%.1f", v)

@Composable
private fun NoticeCard(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    val accent = Color(0xFFE5484D)
    val dark = LocalDark.current
    val bg = if (dark) accent.copy(alpha = 0.16f) else accent.copy(alpha = 0.08f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(13.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "!",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = if (dark) Color(0xFFF2C7C7) else Color(0xFF8C2F33),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (text.length > 40) {
                Spacer(Modifier.width(6.dp))
                Text(
                    if (expanded) "收起" else "更多",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

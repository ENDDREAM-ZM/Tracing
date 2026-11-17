package com.enddream.tracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enddream.tracing.data.MetroLineRow
import com.enddream.tracing.data.MetroTime
import com.enddream.tracing.data.MetroTimetable

@Composable
fun MetroTimetableScreen(
    line: MetroLineRow,
    times: Map<String, List<MetroTime>>,
    siNames: Map<String, String>,
    selfLineIds: Set<String>,
    minuteOfDay: Int,
    initialReverse: Boolean,
    onBack: () -> Unit
) {
    val ownIds = line.stations.map { it.si }.toSet()
    val perStation = line.stations.associate { st ->
        st.si to times[st.si].orEmpty()
            .filter { (selfLineIds.isEmpty() || it.lineId in selfLineIds) && it.toward in ownIds }
    }
    val entries = perStation.values.flatten()
    val head = line.stations.firstOrNull()
    val tail = line.stations.lastOrNull()
    val forwardOk = head != null && tail != null && entries.any { it.toward == tail.si }
    val backwardOk = head != null && tail != null && entries.any { it.toward == head.si }
    var reverse by remember(line.fullName, initialReverse) {
        mutableStateOf(if (initialReverse && backwardOk) true else false)
    }
    var tab by remember(line.fullName) { mutableStateOf(0) }
    val peak = MetroTimetable.isPeak(minuteOfDay)
    val terminalSi = if (reverse) head?.si.orEmpty() else tail?.si.orEmpty()
    val ordered = if (reverse) line.stations.reversed() else line.stations
    val lineColor = line.color.toLineColor()
    val hourNow = minuteOfDay / 60

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
                        displayMetroLine(line.fullName),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "共${listOf(forwardOk, backwardOk).count { it }}个方向",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            if (!forwardOk && !backwardOk) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "该线路暂无首末班数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                return@Column
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (forwardOk) {
                    DirectionTab(
                        label = "往「${tail?.name.orEmpty()}」方向",
                        selected = !reverse,
                        onClick = { reverse = false }
                    )
                }
                if (backwardOk) {
                    DirectionTab(
                        label = "往「${head?.name.orEmpty()}」方向",
                        selected = reverse,
                        onClick = { reverse = true }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabLabel("到站时刻表", tab == 0, Modifier.weight(1f)) { tab = 0 }
                TabLabel("官方运营时间", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (tab == 0) {
                    HourGroups(
                        ordered = ordered,
                        perStation = perStation,
                        terminalSi = terminalSi,
                        peak = peak,
                        hourNow = hourNow,
                        lineColor = lineColor
                    )
                } else {
                    OperateList(
                        ordered = ordered,
                        perStation = perStation,
                        terminalSi = terminalSi
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DirectionTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val fg = if (selected) MaterialTheme.colorScheme.primary else TextSecondary
    Row(
        Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
private fun TabLabel(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) TextPrimary else TextMuted
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(46.dp)
                .height(3.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun HourGroups(
    ordered: List<com.enddream.tracing.data.MetroStationRow>,
    perStation: Map<String, List<MetroTime>>,
    terminalSi: String,
    peak: Boolean,
    hourNow: Int,
    lineColor: Color
) {
    val groups = remember(ordered, perStation, terminalSi, peak) {
        val map = LinkedHashMap<Int, MutableList<String>>()
        ordered.forEach { st ->
            val t = perStation[st.si].orEmpty().firstOrNull { it.toward == terminalSi } ?: return@forEach
            MetroTimetable.timeline(t.first, t.last, peak).forEach { clock ->
                val h = clock.substringBefore(":").toIntOrNull() ?: return@forEach
                map.getOrPut(h) { mutableListOf() }.add("${st.name} $clock")
            }
        }
        map.toList().sortedBy { it.first }
    }
    var openHour by remember(groups) {
        mutableStateOf(groups.firstOrNull { it.first == hourNow }?.first ?: groups.firstOrNull()?.first ?: -1)
    }
    groups.forEach { (hour, items) ->
        val open = hour == openHour
        val now = hour == hourNow
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { openHour = if (open) -1 else hour }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    String.format("%02d:00-%02d:00", hour, (hour + 1) % 24),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (now) MaterialTheme.colorScheme.primary else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted
                )
            }
            if (open) {
                items.forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(14.dp)
                                .background(lineColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            row,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun OperateList(
    ordered: List<com.enddream.tracing.data.MetroStationRow>,
    perStation: Map<String, List<MetroTime>>,
    terminalSi: String
) {
    ordered.forEach { st ->
        val t = perStation[st.si].orEmpty().firstOrNull { it.toward == terminalSi }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                st.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(Modifier.width(72.dp)) { TimeBadge(true, clockOf(t?.first), 10) }
            Box(Modifier.width(72.dp)) { TimeBadge(false, clockOf(t?.last), 10) }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    }
}

private val clockPattern = Regex("^\\d{1,2}:\\d{2}$")
private val compactPattern = Regex("^\\d{4}$")

private fun clockOf(raw: String?): String {
    val t = raw?.trim().orEmpty()
    if (clockPattern.matches(t)) return t
    if (compactPattern.matches(t)) return t.substring(0, 2) + ":" + t.substring(2)
    return "—"
}

private fun String.toLineColor(): Color = runCatching {
    if (isBlank()) Color(0xFF4FD6C0) else Color(("FF" + this).toLong(16))
}.getOrDefault(Color(0xFF4FD6C0))

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
import androidx.compose.foundation.layout.size
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
    official: List<com.enddream.tracing.data.MetroText.Official>,
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
    val head = line.stations.firstOrNull()
    val tail = line.stations.lastOrNull()
    val officialByName = official.associateBy { it.name }
    val officialByIndex = official.takeIf { it.size == line.stations.size }
    val hasOfficial = official.isNotEmpty()
    val entries = perStation.values.flatten()
    val forwardOk = hasOfficial || (head != null && tail != null && entries.any { it.toward == tail.si })
    val backwardOk = hasOfficial || (head != null && tail != null && entries.any { it.toward == head.si })
    var reverse by remember(line.fullName, initialReverse) {
        mutableStateOf(if (initialReverse && backwardOk) true else false)
    }
    val lineColor = line.color.toLineColor()
    val ordered = if (reverse) line.stations.reversed() else line.stations
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
                        "各站首末班（官方）",
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
                    .padding(start = 18.dp, end = 18.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "站点",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.width(74.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "首班",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                Box(Modifier.width(74.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "末班",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
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
                ordered.forEach { st ->
                    val trueIndex = line.stations.indexOfFirst { it.si == st.si }
                    val byIndex = if (trueIndex >= 0) officialByIndex?.getOrNull(trueIndex) else null
                    val off = byIndex ?: officialByName[st.name]
                    val fromOfficial = if (off != null) {
                        if (reverse) off.backwardFirst to off.backwardLast
                        else off.forwardFirst to off.forwardLast
                    } else null
                    val terminalSi = if (reverse) head?.si.orEmpty() else tail?.si.orEmpty()
                    val t = perStation[st.si].orEmpty()
                        .firstOrNull { it.toward == terminalSi }
                    val firstText = fromOfficial?.first?.ifBlank { null } ?: clockOf(t?.first)
                    val lastText = fromOfficial?.second?.ifBlank { null } ?: clockOf(t?.last)
                    val isCurrent = isNowHour(firstText, lastText, hourNow)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(
                                    if (isCurrent) lineColor else lineColor.copy(alpha = 0.35f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            st.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(Modifier.width(74.dp), contentAlignment = Alignment.Center) {
                            Text(
                                firstText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        }
                        Box(Modifier.width(74.dp), contentAlignment = Alignment.Center) {
                            Text(
                                lastText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun isNowHour(first: String, last: String, hourNow: Int): Boolean {
    val f = first.substringBefore(":").toIntOrNull() ?: return false
    val l = last.substringBefore(":").toIntOrNull() ?: return false
    val lh = if (l < f) l + 24 else l
    val h = if (hourNow < f) hourNow + 24 else hourNow
    return h in f..lh
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

private val clockPattern = Regex("^\\d{1,2}:\\d{2}$")
private val compactPattern = Regex("^\\d{4}$")

private fun clockOf(raw: String?): String {
    val t = raw?.trim().orEmpty().removePrefix("次日")
    if (clockPattern.matches(t)) return t
    if (compactPattern.matches(t)) return t.substring(0, 2) + ":" + t.substring(2)
    return "—"
}

private fun String.toLineColor(): Color = runCatching {
    if (isBlank()) Color(0xFF4FD6C0) else Color(("FF" + this).toLong(16))
}.getOrDefault(Color(0xFF4FD6C0))

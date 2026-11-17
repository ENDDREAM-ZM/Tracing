package com.enddream.tracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enddream.tracing.data.RailRow

private val BellW = 44.dp
private val BarW = 26.dp
private val CylW = 13.dp
private val RailGap = 12.dp
private val StationPad = 6.dp

internal fun railColor(token: String): Color {
    val t = token.trim()
    if (t.length == 6 && t.all { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' }) {
        return runCatching { Color(("FF" + t).toLong(16)) }.getOrDefault(Color(0xFFB9BFC6))
    }
    return when (t.lowercase()) {
        "green" -> Color(0xFF22C55E)
        "orange", "yellow" -> Color(0xFFFACC15)
        "red" -> Color(0xFFEF4444)
        else -> Color(0xFFB9BFC6)
    }
}

@Composable
fun SpeedLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf("green" to "畅通", "orange" to "缓慢", "red" to "拥堵").forEach { (co, label) ->
            Box(Modifier.size(7.dp).drawBehind { drawCircle(railColor(co)) })
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.width(9.dp))
        }
    }
}

@Composable
fun RailList(
    rows: List<RailRow>,
    alertStation: String?,
    onAlert: (String) -> Unit,
    modifier: Modifier = Modifier,
    alertable: Boolean = true,
    alertLabel: String = "",
    lineColor: Color? = null,
    highlightStation: String? = null,
    listState: LazyListState = rememberLazyListState(),
    onStationClick: ((RailRow.Station) -> Unit)? = null
) {
    LazyColumn(modifier.fillMaxSize(), state = listState) {
        items(rows.size, key = { i -> "rail-$i-${rows[i].key}" }) { i ->
            when (val r = rows[i]) {
                is RailRow.Station -> StationLineRow(
                    station = r,
                    alerting = alertStation == r.name,
                    alertable = alertable,
                    alertLabel = alertLabel,
                    highlighted = highlightStation != null && highlightStation == r.name,
                    lineColor = lineColor,
                    onClick = onStationClick?.let { cb -> { cb(r) } },
                    onAlert = { onAlert(r.name) }
                )

                is RailRow.Vehicle -> VehicleLineRow(r)
            }
        }
    }
}

@Composable
private fun StationLineRow(
    station: RailRow.Station,
    alerting: Boolean,
    alertable: Boolean,
    alertLabel: String,
    highlighted: Boolean,
    lineColor: Color?,
    onClick: (() -> Unit)?,
    onAlert: () -> Unit
) {
    val ring = lineColor ?: MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.Top
    ) {
        if (alertable) {
            Column(
                Modifier
                    .width(BellW)
                    .padding(vertical = StationPad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onAlert, modifier = Modifier.size(22.dp)) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "到站提醒",
                        modifier = Modifier.size(14.dp),
                        tint = if (alerting) MaterialTheme.colorScheme.primary
                        else TextMuted.copy(alpha = 0.4f)
                    )
                }
                if (alertLabel.isNotBlank()) {
                    Text(
                        alertLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (alerting) MaterialTheme.colorScheme.primary else TextMuted,
                        maxLines = 1
                    )
                }
            }
        } else {
            Spacer(Modifier.width(BellW))
        }

        Box(
            Modifier
                .width(BarW)
                .fillMaxHeight()
                .drawBehind {
                    val cx = size.width / 2f
                    val half = CylW.toPx() / 2f
                    val pad = StationPad.toPx()
                    val top = if (station.first) pad else 0f
                    val bottom = if (station.last) size.height - pad else size.height
                    val upper = if (station.first) pad else 0f
                    val lower = if (station.last) size.height - pad else size.height
                    if (station.before != "none" && top < lower) {
                        drawRect(
                            railColor(station.before),
                            Offset(cx - half, upper),
                            Size(half * 2f, size.height / 2f - upper)
                        )
                    }
                    if (station.after != "none" && upper < bottom) {
                        drawRect(
                            railColor(station.after),
                            Offset(cx - half, size.height / 2f),
                            Size(half * 2f, lower - size.height / 2f)
                        )
                    }
                }
        )

        Spacer(Modifier.width(RailGap))

        Column(
            Modifier
                .weight(1f)
                .padding(vertical = StationPad)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val cx = -(RailGap.toPx() + BarW.toPx() / 2f)
                        val cy = size.height / 2f
                        val r = 6.dp.toPx()
                        drawCircle(
                            Color.Black.copy(alpha = 0.10f),
                            r + 1.5.dp.toPx(),
                            Offset(cx, cy + 1.dp.toPx())
                        )
                        drawCircle(Color.White, r, Offset(cx, cy))
                        drawCircle(ring, r, Offset(cx, cy), style = Stroke(2.dp.toPx()))
                    }
                    .then(
                        if (highlighted) Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp)
                        else Modifier
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        station.name,
                        fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (station.transfers.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        station.transfers.take(3).forEach { tf ->
                            Box(
                                Modifier
                                    .padding(end = 4.dp)
                                    .background(railColor(tf.color), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    tf.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            if (station.times.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    station.times.take(2).forEachIndexed { idx, t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (idx == 0) "第1辆 " else "第2辆 ",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1
                            )
                            Text(
                                t,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (idx == 0) MaterialTheme.colorScheme.primary else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            if (station.door.isNotBlank()) {
                Text(
                    station.door,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VehicleLineRow(r: RailRow.Vehicle) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(BellW))
        Box(
            Modifier
                .width(BarW)
                .fillMaxHeight()
                .drawBehind {
                    val cx = size.width / 2f
                    val half = CylW.toPx() / 2f
                    drawRect(
                        railColor(r.color),
                        Offset(cx - half, 0f),
                        Size(half * 2f, size.height)
                    )
                }
        )
        Spacer(Modifier.width(RailGap))
        Box(
            Modifier
                .background(railColor(r.color).copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                r.detail,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                color = railColor(r.color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

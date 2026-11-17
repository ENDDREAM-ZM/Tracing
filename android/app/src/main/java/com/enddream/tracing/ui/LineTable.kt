package com.enddream.tracing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LineColW = 50.dp
val FavColW = 30.dp

fun displayLine(name: String): String {
    val t = name.trim().trimEnd('_', '-', '—', '·', ' ')
    return t.ifBlank { name.trim() }
}

@Composable
fun LineTableHeader(
    showLineCol: Boolean = true,
    withFav: Boolean = false,
    showCurrentCol: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLineCol) {
            Text(
                "线路",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.width(LineColW)
            )
        }
        CellLabel("起始站")
        if (showCurrentCol) CellLabel("当前站")
        CellLabel("终点站")
        if (withFav) Spacer(Modifier.width(FavColW))
    }
}

@Composable
private fun RowScope.CellLabel(t: String) {
    Text(
        t,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f)
    )
}

@Composable
fun LineTableRow(
    line: String,
    from: String,
    current: String,
    to: String,
    showLineCol: Boolean = true,
    showCurrentCol: Boolean = true,
    onOpen: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLineCol) {
            Box(Modifier.width(LineColW)) {
                Text(
                    displayLine(line),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        CellText(from)
        if (showCurrentCol) {
            CellText(
                current.ifBlank { "—" },
                color = if (current.isBlank()) TextMuted else MaterialTheme.colorScheme.primary
            )
        }
        CellText(to)
        trailing?.invoke()
    }
}

@Composable
private fun RowScope.CellText(value: String, color: Color = TextSecondary) {
    Text(
        value.ifBlank { "—" },
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 1.dp)
    )
}

@Composable
fun TableDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
fun FavHeart(fav: Boolean, onClick: () -> Unit, size: Int = 26) {
    FavButton(fav = fav, onClick = onClick, modifier = Modifier.size(size.dp))
}

@Composable
fun LineTableCard(
    title: String,
    subtitle: String,
    loading: Boolean,
    lines: List<CardLine>,
    favKeys: Set<String> = emptySet(),
    firstLast: Pair<String, String>? = null,
    moreCount: Int = 0,
    showLineCol: Boolean = true,
    onOpenStation: () -> Unit,
    onOpenLine: (CardLine) -> Unit,
    onFav: (CardLine) -> Unit,
    onGo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier
                        .size(15.dp)
                        .clickable(onClick = onOpenStation),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(5.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenStation)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (firstLast != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TimeBadge(true, firstLast.first, 9)
                            TimeBadge(false, firstLast.second, 9)
                        }
                    }
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    "距离$subtitle",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                if (onGo != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clickable(onClick = onGo)
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "全部线路 ›",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            when {
                loading -> Text(
                    "加载中…",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 14.dp, bottom = 12.dp)
                )

                lines.isEmpty() -> Text(
                    "暂无线路",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 14.dp, bottom = 12.dp)
                )

                else -> {
                    Spacer(Modifier.height(4.dp))
                    LineTableHeader(showLineCol)
                    lines.forEach { cl ->
                        TableDivider()
                        LineTableRow(
                            line = cl.lineName,
                            from = cl.from,
                            current = cl.current,
                            to = cl.to,
                            showLineCol = showLineCol,
                            onOpen = { onOpenLine(cl) }
                        )
                    }
                    if (moreCount > 0) {
                        Text(
                            "还有 $moreCount 条线路",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 14.dp, top = 4.dp, bottom = 10.dp)
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

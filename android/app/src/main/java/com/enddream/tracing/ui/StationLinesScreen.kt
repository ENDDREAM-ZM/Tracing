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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enddream.tracing.data.api.BusLine

@Composable
fun StationLinesScreen(
    city: String,
    station: String,
    lng: Double,
    lat: Double,
    onBack: () -> Unit,
    onPickLine: (BusLine) -> Unit,
    onGo: () -> Unit,
    vm: StationLinesViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(city, station, lng, lat) { vm.load(city, station, lng, lat) }

    LaunchedEffect(state.lines.size) {
        if (state.lines.size == 1) onPickLine(state.lines.first())
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
                    .padding(horizontal = 6.dp, vertical = 6.dp),
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
                        station,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "选择经过本站的线路",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(30.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    state.lines.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.error ?: "该站点暂无线路",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }

                    else -> GlassPanel(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            itemsIndexed(state.lines, key = { i, l -> "sl-$i-${l.lineName}-${l.upperOrDown}" }) { _, line ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPickLine(line) }
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            displayLine(line.lineName),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            "${line.from} → ${line.to}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    GlassChip(corner = 10.dp, onClick = onGo) {
                                        Text(
                                            "前往",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "›",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextMuted.copy(alpha = 0.7f)
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

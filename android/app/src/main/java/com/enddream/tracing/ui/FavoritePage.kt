package com.enddream.tracing.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enddream.tracing.data.Favorite

@Composable
fun FavoritePage(
    favorites: List<Favorite>,
    cards: List<FavoriteCard>,
    onOpen: (Favorite) -> Unit,
    onRemove: (Favorite) -> Unit,
    onPullRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = TextPrimary.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "还没有收藏的线路",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "在公交、地铁页进行收藏",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            PullRefresh(
                refreshing = cards.isNotEmpty() && cards.any { it.loading },
                onRefresh = onPullRefresh
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = LocalTopInset.current + 8.dp,
                        bottom = LocalBottomInset.current + 8.dp
                    )
                ) {
                    itemsIndexed(
                        cards,
                        key = {
                            i, c ->
                            "fav-$i-${c.favorite.kind}-${c.favorite.city}-${c.favorite.line}-${c.favorite.direction}"
                        }
                    ) { _, card ->
                        val fav = card.favorite
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Place,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (fav.kind == "metro") "${displayLine(fav.line)} · ${fav.from} → ${fav.to}"
                                        else displayLine(fav.line),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FavHeart(fav = true, onClick = { onRemove(fav) }, size = 24)
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        fav.city,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }
                                if (fav.kind == "metro") {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${fav.from} → ${fav.to}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "地铁",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                } else {
                                    LineTableHeader(showLineCol = false)
                                    TableDivider()
                                    LineTableRow(
                                        line = fav.line,
                                        from = card.favorite.from,
                                        current = card.current,
                                        to = card.favorite.to,
                                        showLineCol = false,
                                        onOpen = { onOpen(fav) }
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        }
    }
}

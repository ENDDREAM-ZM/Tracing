package com.enddream.tracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class MainTab(val label: String, val icon: ImageVector) {
    BUS("公交", Icons.Filled.Place),
    METRO("地铁", Icons.Filled.Menu),
    NAV("导航", Icons.Filled.Share),
    FAVORITE("收藏", Icons.Filled.Favorite),
    SETTINGS("设置", Icons.Filled.Settings)
}

@Composable
fun MainScaffold(
    tab: MainTab,
    onTabChange: (MainTab) -> Unit,
    city: String,
    onPickCity: () -> Unit,
    floatBar: Boolean,
    content: @Composable () -> Unit
) {
    val sysTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val sysBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = sysTop)
        ) {
            GlassCapsule(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "追迹",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onPickCity),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            city,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "›",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            Box(Modifier.weight(1f)) { content() }

            if (floatBar) {
                Spacer(Modifier.height(82.dp + sysBottom))
            } else {
                BottomBarInner(tab, onTabChange)
            }
        }

        if (floatBar) GlassCapsule(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 14.dp, end = 14.dp, bottom = sysBottom + 12.dp)
                .fillMaxWidth()
                .height(62.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MainTab.entries.forEach { item ->
                    TabItem(
                        item = item,
                        selected = item == tab,
                        onClick = { onTabChange(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarInner(tab: MainTab, onTabChange: (MainTab) -> Unit) {
    val sysBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp + sysBottom)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.entries.forEach { item ->
                TabItem(
                    item = item,
                    selected = item == tab,
                    onClick = { onTabChange(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    item: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = MaterialTheme.colorScheme.primary
    val idle = TextMuted
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else Color.Transparent,
                    shape = RoundedCornerShape(13.dp)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Icon(
                item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(16.dp),
                tint = if (selected) active else idle
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) active else idle,
            maxLines = 1
        )
    }
}

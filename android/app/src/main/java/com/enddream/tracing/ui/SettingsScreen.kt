package com.enddream.tracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    city: String,
    refreshSec: Int,
    notifyOn: Boolean,
    themeMode: String,
    floatBar: Boolean,
    onPickCity: () -> Unit,
    onRefreshChange: (Int) -> Unit,
    onNotifyChange: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onFloatBarChange: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
) {
    var showSources by remember { mutableStateOf(false) }
    var showThanks by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val versionName = runCatching {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        "${pi.versionName} (vc${pi.longVersionCode.toInt()})"
    }.getOrDefault("")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Group("外观") {
            LabeledRowSeg("主题") {
                SegRow(
                    listOf("auto" to "跟随系统", "light" to "浅色", "dark" to "暗色"),
                    themeMode
                ) { onThemeChange(it) }
            }
            RowDivider()
            ToggleRow(
                title = "悬浮底栏",
                checked = floatBar,
                onChange = onFloatBarChange
            )
        }

        Group("数据") {
            CityButton(city = city, onClick = onPickCity)
            RowDivider()
            LabeledRowSeg("刷新频率") {
                SegRow(listOf("10" to "10 秒", "15" to "15 秒", "30" to "30 秒"), refreshSec.toString()) {
                    onRefreshChange(it.toInt())
                }
            }
        }

        Group("提醒") {
            ToggleRow(
                title = "消息提醒",
                subtitle = "车辆到站前提醒并振动",
                checked = notifyOn,
                onChange = onNotifyChange
            )
        }

        Group("关于") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "追迹",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(versionName, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showThanks = true }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "致谢",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "查看",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showSources = true }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "数据来源",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "查看",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPermissions)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "权限说明",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "全部权限与用途",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { openUrl(ctx, "https://github.com/ENDDREAM-ZM/Tracing") }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "项目地址",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "github.com/ENDDREAM-ZM/Tracing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(120.dp))
    }

    if (showSources) {
        AlertDialog(
            onDismissRequest = { showSources = false },
            confirmButton = {
                TextButton(onClick = { showSources = false }) { Text("知道了") }
            },
            title = { Text("数据来源") },
            text = {
                Column {
                    listOf(
                        "公交数据" to "掌上公交 h5.mygolbs.com",
                        "地铁线网" to "高德地图 webapi.amap.com",
                        "地铁首末班" to "高德地图 subway/data",
                        "地铁城市表" to "掌上公交 metro.mygolbs.com",
                        "定位" to "Android 系统定位服务",
                        "导航" to "已安装的地图应用"
                    ).forEach { (k, v) ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(
                                k,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                v,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        )
    }

    if (showThanks) {
        AlertDialog(
            onDismissRequest = { showThanks = false },
            confirmButton = {
                TextButton(onClick = { showThanks = false }) { Text("关闭") }
            },
            title = { Text("致谢") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ThanksItem("掌上公交", "公交线路、站点与实时车辆数据", "https://h5.mygolbs.com")
                    ThanksItem("高德地图", "地铁线网、首末班数据", "https://webapi.amap.com")
                    ThanksItem("Jetpack Compose", "Android 界面框架", "https://developer.android.com/jetpack/compose")
                    ThanksItem("Kotlin", "开发语言", "https://kotlinlang.org")
                    ThanksItem("OkHttp", "网络请求", "https://square.github.io/okhttp")
                    ThanksItem("kotlinx.serialization", "JSON 解析", "https://github.com/Kotlin/kotlinx.serialization")
                    ThanksItem("Material Icons", "图标", "https://fonts.google.com/icons")
                }
            }
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 7.dp)
    )
    GlassPanel(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )

}

@Composable
private fun ThanksItem(title: String, desc: String, url: String) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { openUrl(ctx, url) }
            .padding(vertical = 5.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            desc,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun CityButton(city: String, onClick: () -> Unit) {
    Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "默认城市",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    city,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "更换",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(2.dp))
            Text(
                "›",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun LabeledRowSeg(label: String, content: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RowScope.SegRow(
    options: List<Pair<String, String>>,
    current: String,
    onPick: (String) -> Unit
) {
    options.forEach { (key, label) ->
        val selected = current == key
        Box(
            Modifier
                .weight(1f)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else ChipIdle,
                    RoundedCornerShape(10.dp)
                )
                .clickable { onPick(key) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                maxLines = 1
            )
        }
    }
}

val TextPrimary: Color
    @Composable get() = if (LocalDark.current) Color(0xFFE8EAEC) else Color(0xFF1A1C1B)

val TextSecondary: Color
    @Composable get() = if (LocalDark.current) Color(0xFFB6BCC2) else Color(0xFF4A504E)

val TextMuted: Color
    @Composable get() = if (LocalDark.current) Color(0xFF868D93) else Color(0xFF868C8A)

val ChipIdle: Color
    @Composable get() = if (LocalDark.current) Color(0xFF2F3337) else Color(0xFFEDEFEE)

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
    amapAgree: Boolean,
    themeMode: String,
    floatBar: Boolean,
    onPickCity: () -> Unit,
    onRefreshChange: (Int) -> Unit,
    onNotifyChange: (Boolean) -> Unit,
    onAmapChange: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onFloatBarChange: (Boolean) -> Unit,
) {
    var showSources by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var showPerms by remember { mutableStateOf(false) }
    var showGeo by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val versionName = runCatching {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        pi.versionName.orEmpty()
    }.getOrDefault("")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(LocalTopInset.current))
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

        Group("定位") {
            ToggleRow(
                title = "高德定位",
                subtitle = "使用高德定位SDK来进行定位，关闭后使用系统定位",
                checked = amapAgree,
                onChange = onAmapChange
            )
            RowDivider()
            LinkRow("位置信息说明", "查看") { showGeo = true }
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
            LinkRow("数据来源", "查看") { showSources = true }
            RowDivider()
            LinkRow("权限说明", "查看") { showPerms = true }
            RowDivider()
            LinkRow("项目地址", "前往") {
                openUrl(ctx, "https://github.com/ENDDREAM-ZM/Tracing")
            }
            RowDivider()
            LinkRow("开放源代码许可", "查看") { showLicenses = true }
        }

        Spacer(Modifier.height(LocalBottomInset.current + 40.dp))
    }

    if (showPerms) {
        GlassDialog(
            onDismissRequest = { showPerms = false },
            title = "权限说明",
            subtitle = "追迹申请的全部权限及用途",
            buttons = {
                GlassDialogButton("关闭") { showPerms = false }
            }
        ) {
            Column(Modifier.fillMaxWidth()) {
                permList.forEach { p ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                p.why,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            if (p.required) "必需" else "可选",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (p.required) MaterialTheme.colorScheme.primary else TextMuted
                        )
                    }
                }
            }
        }
    }

    if (showGeo) {
        GlassDialog(
            onDismissRequest = { showGeo = false },
            title = "位置信息",
            subtitle = "追迹如何获取和使用位置",
            buttons = {
                GlassDialogButton("知道了") { showGeo = false }
            }
        ) {
            Column {
                listOf(
                    "定位来源" to "高德定位 SDK",
                    "使用目的" to "识别城市、查找附近站点与车辆距离",
                    "收集内容" to "位置、设备与网络信息",
                    "后台使用" to "不在后台定位"
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
    }

    if (showSources) {
        GlassDialog(
            onDismissRequest = { showSources = false },
            title = "数据来源",
            buttons = {
                GlassDialogButton("知道了") { showSources = false }
            }
        ) {
            Column {
                listOf(
                    "公交数据" to "掌上公交",
                    "地铁线网" to "高德地图",
                    "地铁首末班" to "高德地图",
                    "地铁公告" to "高德地图",
                    "地铁城市表" to "掌上公交",
                    "定位" to "高德定位 SDK 与系统定位",
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
    }

    if (showLicenses) {
        GlassDialog(
            onDismissRequest = { showLicenses = false },
            title = "开放源代码许可",
            subtitle = "追迹使用的第三方开源组件",
            buttons = {
                GlassDialogButton("关闭") { showLicenses = false }
            }
        ) {
            Column(Modifier.fillMaxWidth()) {
                licenseList.forEach { item -> LicenseRow(item) }
                Spacer(Modifier.height(4.dp))
                Text(
                    "追迹自身以 MIT 许可证开源",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
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
private fun LinkRow(label: String, hint: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1
        )
    }
}

private data class LicenseEntry(
    val name: String,
    val desc: String,
    val license: String,
    val url: String
)

private val licenseList = listOf(
    LicenseEntry(
        "AndroidLiquidGlass",
        "液态玻璃模糊、高光与阴影效果",
        "Apache-2.0",
        "https://github.com/Kyant0/AndroidLiquidGlass"
    ),
    LicenseEntry(
        "Jetpack Compose",
        "Android 界面框架，含 Material 3 组件库",
        "Apache-2.0",
        "https://developer.android.com/jetpack/compose"
    ),
    LicenseEntry(
        "Material Icons",
        "界面图标",
        "Apache-2.0",
        "https://fonts.google.com/icons"
    ),
    LicenseEntry(
        "AndroidX Core / Activity / Lifecycle",
        "系统能力、页面生命周期与 Compose 集成",
        "Apache-2.0",
        "https://developer.android.com/jetpack/androidx"
    ),
    LicenseEntry(
        "AndroidX DataStore",
        "设置项与收藏的本地存储",
        "Apache-2.0",
        "https://developer.android.com/topic/libraries/architecture/datastore"
    ),
    LicenseEntry(
        "Kotlin",
        "开发语言与标准库",
        "Apache-2.0",
        "https://kotlinlang.org"
    ),
    LicenseEntry(
        "kotlinx.coroutines",
        "协程与异步任务",
        "Apache-2.0",
        "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    LicenseEntry(
        "kotlinx.serialization",
        "JSON 解析",
        "Apache-2.0",
        "https://github.com/Kotlin/kotlinx.serialization"
    ),
    LicenseEntry(
        "OkHttp",
        "网络请求",
        "Apache-2.0",
        "https://square.github.io/okhttp"
    ),
    LicenseEntry(
        "Okio",
        "OkHttp 依赖的 I/O 库",
        "Apache-2.0",
        "https://square.github.io/okio"
    )
)

@Composable
private fun LicenseRow(item: LicenseEntry) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { openUrl(ctx, item.url) }
            .padding(vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                item.license,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Text(
            item.desc,
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
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            shadow = false,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "默认城市",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        city,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "更换",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted
                )
            }
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
        GlassChip(
            modifier = Modifier.weight(1f),
            corner = 11.dp,
            selected = selected,
            onClick = { onPick(key) }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

private data class PermItem(
    val name: String,
    val why: String,
    val required: Boolean
)

private val permList = listOf(
    PermItem("位置信息", "定位所在城市、附近站点与最近车辆", false),
    PermItem("通知", "公交、地铁的到站提醒", false),
    PermItem("振动", "到站提醒时用振动提示", false),
    PermItem("网络", "获取线路、站点、实时车辆与地铁线网数据", true)
)

val TextPrimary: Color
    @Composable get() = if (LocalDark.current) Color(0xFFE8EAEC) else Color(0xFF1A1C1B)

val TextSecondary: Color
    @Composable get() = if (LocalDark.current) Color(0xFFB6BCC2) else Color(0xFF4A504E)

val TextMuted: Color
    @Composable get() = if (LocalDark.current) Color(0xFF868D93) else Color(0xFF868C8A)

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enddream.tracing.data.CityProvince

private val hotProvinces = listOf(
    "北京", "上海", "天津", "重庆", "广东",
    "江苏", "浙江", "福建", "山东", "四川",
    "湖北", "湖南", "河南", "陕西", "辽宁"
)

private val hotCities = listOf(
    "北京市", "上海市", "广州市", "深圳市", "厦门市",
    "福州市", "杭州市", "南京市", "成都市", "重庆市",
    "武汉市", "西安市", "天津市", "苏州市", "宁波市"
)

private fun isCity(name: String): Boolean =
    name.endsWith("市") || name.endsWith("自治州") || name.endsWith("地区")

@Composable
fun CityStartScreen(
    current: String,
    granted: Boolean,
    locatedCity: String,
    locating: Boolean,
    onPick: (String) -> Unit,
    onLocate: () -> Unit
) {
    var cities by remember { mutableStateOf(emptyList<String>()) }
    var keyword by remember { mutableStateOf("") }
    var provinceOpen by remember { mutableStateOf(false) }
    var cityOpen by remember { mutableStateOf(false) }
    var countyOpen by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (cities.isEmpty()) {
            cities = runCatching {
                com.enddream.tracing.data.api.BusApi().cityList()
            }.getOrDefault(emptyList())
        }
    }

    val provinces = remember(cities) {
        (hotProvinces.filter { p -> cities.any { CityProvince.province[it] == p } } +
            cities.mapNotNull { CityProvince.province[it] }.distinct())
            .distinct()
    }
    val inProvince = remember(cities, picked) {
        if (picked.isBlank()) cities
        else cities.filter { CityProvince.province[it] == picked }
    }
    val cityLevel = remember(inProvince) { inProvince.filter { isCity(it) } }
    val countyLevel = remember(inProvince) { inProvince.filter { !isCity(it) } }
    val hot = remember(cities) { hotCities.filter { h -> cities.contains(h) } }
    val hits = remember(cities, keyword) {
        val k = keyword.trim()
        if (k.isBlank()) emptyList()
        else cities.filter { it.contains(k, true) || it.replace("市", "").contains(k, true) }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            GlassPill(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                SearchField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    onSearch = {},
                    placeholder = "输入城市名或拼音首字母…",
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
            Spacer(Modifier.height(10.dp))
            if (granted) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(onClick = onLocate)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (locating) "定位中…" else "定位城市：${locatedCity.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "重新定位",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "开启定位权限可自动选择城市",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(onClick = onLocate)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "去开启",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "列表只包含部分热门地区与已开通公交或地铁数据的地区，未开通的地区不会显示",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        if (keyword.isNotBlank()) {
            item { Grid5(hits, current, onPick) }
            item { Spacer(Modifier.height(20.dp)) }
        } else {
            if (hot.isNotEmpty()) {
                item { SectionHeader("热门地区", false, null) }
                item { Grid5(hot, current, onPick) }
                item { Spacer(Modifier.height(14.dp)) }
            }
            if (provinces.isNotEmpty()) {
                item {
                    SectionHeader("省级行政区", true, provinceOpen) {
                        provinceOpen = !provinceOpen
                    }
                }
                item {
                    Grid5Select(
                        items = if (provinceOpen) provinces else provinces.take(15),
                        selected = picked,
                        onPick = { p -> picked = if (picked == p) "" else p }
                    )
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
            val cityTitle = if (picked.isBlank()) "市级行政区" else "$picked · 城市"
            if (cityLevel.isNotEmpty()) {
                item {
                    SectionHeader(cityTitle, true, cityOpen) { cityOpen = !cityOpen }
                }
                item { Grid5(if (cityOpen) cityLevel else cityLevel.take(15), current, onPick) }
                item { Spacer(Modifier.height(14.dp)) }
            }
            if (countyLevel.isNotEmpty()) {
                item {
                    SectionHeader("市 / 区 / 县", true, countyOpen) { countyOpen = !countyOpen }
                }
                item { Grid5(if (countyOpen) countyLevel else countyLevel.take(15), current, onPick) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    expandable: Boolean,
    open: Boolean?,
    onToggle: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (expandable && onToggle != null) {
            Text(
                if (open == true) "收起" else "展开",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun Grid5(items: List<String>, current: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val selected = name == current
                    Box(
                        Modifier
                            .weight(1f)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onPick(name) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Grid5Select(items: List<String>, selected: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val on = name == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .background(
                                if (on) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onPick(name) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

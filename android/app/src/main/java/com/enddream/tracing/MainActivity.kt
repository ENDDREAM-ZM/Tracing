package com.enddream.tracing

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enddream.tracing.data.Prefs
import com.enddream.tracing.ui.BusPage
import com.enddream.tracing.ui.BusViewModel
import com.enddream.tracing.ui.CardLine
import com.enddream.tracing.ui.CityStartScreen
import com.enddream.tracing.ui.FavoritePage
import com.enddream.tracing.ui.FavoriteViewModel
import com.enddream.tracing.ui.MainScaffold
import com.enddream.tracing.ui.MainTab
import com.enddream.tracing.ui.MapNav
import com.enddream.tracing.ui.MetroPage
import com.enddream.tracing.ui.NavPage
import com.enddream.tracing.ui.PermissionsScreen
import com.enddream.tracing.ui.RouteScreen
import com.enddream.tracing.ui.SettingsScreen
import com.enddream.tracing.ui.StationLinesScreen
import com.enddream.tracing.ui.TracingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface Screen {
    data object Main : Screen

    data class Route(
        val city: String,
        val line: String,
        val direction: Int,
        val from: String,
        val to: String,
        val highlight: String = ""
    ) : Screen

    data class Station(
        val city: String,
        val station: String,
        val lng: Double,
        val lat: Double
    ) : Screen

    data object Permissions : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val crash = TracingApp.lastCrash(application)
        setContent {
            if (crash != null) {
                CrashScreen(crash) {
                    TracingApp.clearCrash(application)
                    recreate()
                }
            } else {
                var themeMode by remember { mutableStateOf("auto") }
                LaunchedEffect(Unit) {
                    themeMode = runCatching { Prefs.themeMode(applicationContext) }
                        .getOrDefault("auto")
                }
                TracingTheme(mode = themeMode) {
                    App(themeMode = themeMode, onThemePicked = { themeMode = it })
                }
            }
        }
    }
}

@Composable
private fun CrashScreen(text: String, onClear: () -> Unit) {
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1C1B))) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text("上次运行崩溃了", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFB4AB))
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF262A29), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BasicText(text, style = TextStyle(fontSize = 11.sp, color = Color(0xFFE3E5E4)))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", text))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("复制崩溃信息") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("清除并重启") }
        }
    }
}

@Composable
private fun App(themeMode: String, onThemePicked: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var tab by remember { mutableStateOf(MainTab.BUS) }
    var city by remember { mutableStateOf("") }
    var entered by remember { mutableStateOf(false) }
    var cityLocked by remember { mutableStateOf(false) }
    var permGranted by remember { mutableStateOf(false) }
    var locatedCity by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(emptyList<com.enddream.tracing.data.Favorite>()) }
    var locateTick by remember { mutableStateOf(0) }
    var autoEnter by remember { mutableStateOf(true) }
    var prefsLoaded by remember { mutableStateOf(false) }
    var refreshSec by remember { mutableStateOf(10) }
    var floatBar by remember { mutableStateOf(true) }
    var notifyOn by remember { mutableStateOf(false) }
    var allCities by remember { mutableStateOf(emptyList<String>()) }

    val busVm: BusViewModel = viewModel()
    val metroVm: com.enddream.tracing.ui.MetroViewModel = viewModel()
    val favVm: FavoriteViewModel = viewModel()

    fun toast(msg: String) {
        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    val navigate: (Double, Double, String) -> Unit = { lat, lng, name ->
        if (!MapNav.openSystem(ctx, lat, lng, name)) toast("未安装地图应用")
    }

    val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun ensureNotifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val ok = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!ok) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun enter(picked: String) {
        city = picked
        entered = true
        autoEnter = false
        locating = false
        cityLocked = true
        scope.launch { Prefs.setCity(ctx, picked) }
        busVm.setCity(picked)
        busVm.loadAll()
        locateTick++
    }

    LaunchedEffect(Unit) {
        Prefs.favoritesFlow(ctx).collect { list ->
            favorites = list
            busVm.setFavorites(list)
            favVm.load(list)
        }
    }

    LaunchedEffect(Unit) {
        allCities = runCatching {
            com.enddream.tracing.data.api.BusApi().cityList()
        }.getOrDefault(emptyList())
    }

    LaunchedEffect(prefsLoaded) {
        if (!prefsLoaded) return@LaunchedEffect
        permGranted = Locate.granted(ctx)
        if (!entered && permGranted) locateTick++
    }

    LaunchedEffect(Unit) {
        val saved = runCatching { Prefs.city(ctx) }.getOrDefault("")
        val picked = runCatching { Prefs.citySet(ctx) }.getOrDefault(false)
        if (picked && saved.isNotBlank()) {
            city = saved
            entered = true
            autoEnter = false
            busVm.setCity(saved)
            busVm.loadAll()
            locateTick++
        }
        refreshSec = runCatching { Prefs.refreshSec(ctx) }.getOrDefault(10)
        floatBar = runCatching { Prefs.floatBar(ctx) }.getOrDefault(true)
        notifyOn = runCatching { Prefs.notifyEnabled(ctx) }.getOrDefault(false)
        if (notifyOn) ensureNotifyPermission()
        prefsLoaded = true
    }

    val locationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        permGranted = grants.values.any { it }
        if (permGranted) locateTick++
    }

    LaunchedEffect(locateTick, prefsLoaded) {
        if (locateTick == 0 || !prefsLoaded) return@LaunchedEffect
        permGranted = Locate.granted(ctx)
        if (!permGranted) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return@LaunchedEffect
        }
        locating = true
        val pos = withContext(Dispatchers.IO) { Locate.current(ctx) }
        if (pos == null) {
            locating = false
            toast("定位失败，请检查系统定位开关")
            return@LaunchedEffect
        }
        busVm.locate(pos.first, pos.second)
        val cities = withTimeoutOrNull(1500) {
            while (allCities.isEmpty()) kotlinx.coroutines.delay(60)
            allCities
        } ?: allCities
        if (cities.isNotEmpty()) {
            val hits = withContext(Dispatchers.IO) { Locate.candidates(ctx, pos.second, pos.first) }
            val hit = hits.firstNotNullOfOrNull { c ->
                cities.firstOrNull { it == c }
                    ?: cities.firstOrNull { it.removeSuffix("市") == c.removeSuffix("市") }
                    ?: cities.firstOrNull { c.contains(it.removeSuffix("市")) && it.removeSuffix("市").length >= 2 }
            }
            if (hit != null) locatedCity = hit
            if (hit != null && hit != city) {
                if (!entered && autoEnter) {
                    enter(hit)
                    locating = false
                    return@LaunchedEffect
                }
                if (entered && !cityLocked) {
                    city = hit
                    scope.launch { Prefs.setCity(ctx, hit) }
                    busVm.setCity(hit)
                    busVm.loadAll()
                    busVm.locate(pos.first, pos.second)
                }
            }
        }
        if (!entered && autoEnter) enter(city.ifBlank { "厦门市" })
        locating = false
    }

    LaunchedEffect(refreshSec) { metroVm.refreshSec = refreshSec }

    LaunchedEffect(tab, refreshSec, entered) {
        if (!entered) return@LaunchedEffect
        if (tab == MainTab.METRO) {
            while (true) {
                kotlinx.coroutines.delay(refreshSec * 1000L)
                metroVm.refresh()
            }
        }
    }

    LaunchedEffect(tab, refreshSec, entered) {
        if (!entered) return@LaunchedEffect
        if (tab == MainTab.BUS) {
            while (true) {
                kotlinx.coroutines.delay(refreshSec * 1000L)
                busVm.refreshNearby()
            }
        }
    }

    if (!entered) {
        CityStartScreen(
            current = city,
            granted = permGranted,
            locatedCity = locatedCity,
            locating = locating,
            onPick = { enter(it) },
            onLocate = {
                autoEnter = false
                locateTick++
            }
        )
        return
    }

    BackHandler(enabled = screen !is Screen.Main) { screen = Screen.Main }

    when (val s = screen) {
        Screen.Main -> {
            val busStateForBar by busVm.state.collectAsStateWithLifecycle()
            MainScaffold(
                tab = tab,
                onTabChange = {
                    tab = it
                    if (it == MainTab.FAVORITE) favVm.load(favorites, refresh = true)
                },
                city = city,
                onPickCity = {
                    autoEnter = false
                    entered = false
                },
                floatBar = floatBar,
                topSlot = if (tab == MainTab.BUS) {
                    { com.enddream.tracing.ui.BusSearchBar(busStateForBar) { busVm.onKeywordChange(it) } }
                } else null
            ) {
                when (tab) {
                    MainTab.BUS -> {
                        val state by busVm.state.collectAsStateWithLifecycle()
                        BusPage(
                            state = state,
                            city = city,
                            onKeyword = { busVm.onKeywordChange(it) },
                            onOpenLine = { cl: CardLine ->
                                screen = Screen.Route(
                                    city, cl.lineName, cl.direction, cl.from, cl.to
                                )
                            },
                            onOpenStation = { st ->
                                screen = Screen.Station(
                                    city,
                                    st.name,
                                    st.lon.toDoubleOrNull() ?: 0.0,
                                    st.lat.toDoubleOrNull() ?: 0.0
                                )
                            },
                            onOpenCityLine = { ln ->
                                screen = Screen.Route(
                                    city,
                                    ln.lineName,
                                    ln.upperOrDown.toIntOrNull() ?: 1,
                                    ln.from,
                                    ln.to
                                )
                            },
                            onLocate = { locateTick++ },
                            onPullRefresh = {
                                busVm.refresh()
                                locateTick++
                            },
                            onLoadCurrent = { ln, d -> busVm.loadOtherCurrent(ln, d) }
                        )
                    }

                    MainTab.METRO -> MetroPage(
                        city = city,
                        notifyOn = notifyOn,
                        onAskNotify = { ensureNotifyPermission() },
                        onNavigate = { name, lat, lng -> navigate(lat, lng, name) },
                        vm = metroVm
                    )

                    MainTab.NAV -> {
                        val busState by busVm.state.collectAsStateWithLifecycle()
                        NavPage(
                            city = city,
                            nearby = busState.nearbyDetail.map { it.station },
                            onOpen = { lat, lng, name -> navigate(lat, lng, name) },
                            onLocate = { locateTick++ }
                        )
                    }

                    MainTab.SETTINGS -> SettingsScreen(
                        city = city,
                        refreshSec = refreshSec,
                        notifyOn = notifyOn,
                        onPickCity = {
                            autoEnter = false
                            entered = false
                        },
                        onRefreshChange = { sec ->
                            refreshSec = sec
                            scope.launch { Prefs.setRefreshSec(ctx, sec) }
                        },
                        onNotifyChange = { on ->
                            notifyOn = on
                            scope.launch { Prefs.setNotifyEnabled(ctx, on) }
                            if (on) ensureNotifyPermission()
                        },
                        themeMode = themeMode,
                        floatBar = floatBar,
                        onThemeChange = { m ->
                            onThemePicked(m)
                            scope.launch { Prefs.setThemeMode(ctx, m) }
                        },
                        onFloatBarChange = { on ->
                            floatBar = on
                            scope.launch { Prefs.setFloatBar(ctx, on) }
                        },
                        onOpenPermissions = { screen = Screen.Permissions }
                    )

                    MainTab.FAVORITE -> {
                        val cards by favVm.cards.collectAsStateWithLifecycle()
                        FavoritePage(
                            favorites = favorites,
                            cards = cards,
                            onOpen = { fav ->
                                if (fav.kind == "metro") {
                                    tab = MainTab.METRO
                                    metroVm.selectLineByName(fav.line, fav.direction == 2)
                                } else {
                                    screen = Screen.Route(
                                        fav.city, fav.line, fav.direction, fav.from, fav.to
                                    )
                                }
                            },
                            onPullRefresh = { favVm.load(favorites, refresh = true) },
                            onRemove = { fav ->
                                scope.launch {
                                    Prefs.toggleFavorite(ctx, fav)
                                    toast("已取消收藏该线路")
                                }
                            },
                        )
                    }
                }
            }
        }

        is Screen.Route -> RouteScreen(
            city = s.city,
            line = s.line,
            direction = s.direction,
            from = s.from,
            to = s.to,
            refreshSec = refreshSec,
            notifyOn = notifyOn,
            highlightStation = s.highlight.ifBlank { null },
            onNavigate = { lat, lng, name -> navigate(lat, lng, name) },
            onBack = { screen = Screen.Main }
        )

        Screen.Permissions -> PermissionsScreen(onBack = { screen = Screen.Main })

        is Screen.Station -> StationLinesScreen(
            city = s.city,
            station = s.station,
            lng = s.lng,
            lat = s.lat,
            onBack = { screen = Screen.Main },
            onGo = { navigate(s.lat, s.lng, s.station) },
            onPickLine = { line ->
                screen = Screen.Route(
                    s.city,
                    line.lineName,
                    line.upperOrDown.toIntOrNull() ?: 1,
                    line.from,
                    line.to,
                    s.station
                )
            }
        )
    }
}

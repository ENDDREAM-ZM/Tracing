package com.enddream.tracing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enddream.tracing.data.Favorite
import com.enddream.tracing.data.RouteCache
import com.enddream.tracing.data.RouteRows
import com.enddream.tracing.data.api.BusApi
import com.enddream.tracing.data.api.BusLine
import com.enddream.tracing.data.api.NearbyStation
import com.enddream.tracing.data.api.RealBus
import com.enddream.tracing.data.api.RouteDetail
import com.enddream.tracing.data.api.SpeedItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class CardLine(
    val lineName: String,
    val direction: Int,
    val from: String,
    val to: String,
    val stationOrder: Int = 0,
    val current: String = ""
) {
    val key: String get() = "$lineName|$direction"
}

data class NearbyWithLines(
    val station: NearbyStation,
    val lines: List<CardLine> = emptyList(),
    val loading: Boolean = true,
    val firstLast: Pair<String, String>? = null
)

data class BusUiState(
    val keyword: String = "",
    val nearbyDetail: List<NearbyWithLines> = emptyList(),
    val lines: List<BusLine> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val loading: Boolean = false,
    val loadingNearby: Boolean = false,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val message: String? = null
)

class BusViewModel(private val api: BusApi = BusApi()) : ViewModel() {

    private val _state = MutableStateFlow(BusUiState())
    val state: StateFlow<BusUiState> = _state.asStateFlow()

    private var city: String = "厦门市"
    private var searchJob: Job? = null
    private var nearbyJob: Job? = null
    private var currentJob: Job? = null
    private var ends: Map<String, Pair<String, String>> = emptyMap()
    private var lastLng: Double = 0.0
    private var lastLat: Double = 0.0

    fun setCity(newCity: String) {
        if (newCity == city) return
        city = newCity
        ends = emptyMap()
        _state.value = BusUiState(favorites = _state.value.favorites)
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, refreshing = false, message = null)
            val all = runCatching { api.allLines(city) }.getOrElse {
                _state.value = _state.value.copy(loading = false, message = "加载失败：${it.message}")
                return@launch
            }
            ends = all.associate {
                "${it.lineName}|${it.upperOrDown.toIntOrNull() ?: 1}" to (it.from to it.to)
            }
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                lines = all,
                loaded = true
            )
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(refreshing = true)
        if (_state.value.keyword.isBlank()) {
            loadAll()
            if (lastLng != 0.0) loadNearby(lastLng, lastLat)
        } else {
            onKeywordChange(_state.value.keyword)
        }
    }

    fun locate(lng: Double, lat: Double) {
        lastLng = lng
        lastLat = lat
        _state.value = _state.value.copy(refreshing = true)
        loadNearby(lng, lat)
    }

    fun onKeywordChange(kw: String) {
        _state.value = _state.value.copy(keyword = kw)
        searchJob?.cancel()
        if (kw.isBlank()) {
            loadAll()
            return
        }
        searchJob = viewModelScope.launch {
            delay(280)
            _state.value = _state.value.copy(loading = true)
            val res = runCatching { api.searchLine(city, kw) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(loading = false, lines = res)
        }
    }

    fun loadNearby(lng: Double, lat: Double) {
        lastLng = lng
        lastLat = lat
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            _state.value = _state.value.copy(loadingNearby = true)
            val res = runCatching { api.nearby(city, lng, lat) }.getOrDefault(emptyList())
            val top = res.filter { it.dis <= 2000 }.take(3)
            _state.value = _state.value.copy(
                loadingNearby = false,
                refreshing = false,
                nearbyDetail = top.map { NearbyWithLines(it) }
            )
            top.forEach { st -> launch { fillStation(st) } }
        }
    }

    private var roundRobin: Int = 0

    fun refreshNearby() {
        if (lastLng == 0.0) return
        val current = _state.value.nearbyDetail
        if (current.isEmpty()) return
        currentJob?.cancel()
        val i = roundRobin % current.size
        roundRobin = (roundRobin + 1) % current.size
        currentJob = viewModelScope.launch { updateCurrent(current[i].station, current[i].lines) }
    }

    fun refreshAllCurrent() {
        if (lastLng == 0.0) return
        val current = _state.value.nearbyDetail
        if (current.isEmpty()) return
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            current.forEach { item -> launch { updateCurrent(item.station, item.lines) } }
        }
    }

    private suspend fun updateCurrent(station: NearbyStation, lines: List<CardLine>) {
        if (lines.isEmpty()) return
        val updated = coroutineScope {
            lines.map { cl ->
                async {
                    val cur = currentOf(cl)
                    if (cur.isBlank()) cl else cl.copy(current = cur)
                }
            }.awaitAll()
        }
        val list = _state.value.nearbyDetail.toMutableList()
        val i = list.indexOfFirst { it.station.name == station.name }
        if (i >= 0) {
            list[i] = list[i].copy(lines = updated)
            _state.value = _state.value.copy(nearbyDetail = list)
        }
    }

    private suspend fun currentOf(cl: CardLine): String {
        val order = cl.stationOrder
        if (order <= 0) return cl.current
        val res = runCatching {
            api.realtime(city, cl.lineName, cl.direction, order)
        }.getOrNull() ?: return cl.current
        val nearest = res.listBuses
            .filter { it.index in 0..(order - 1) }
            .maxByOrNull { it.index }
        if (nearest != null && nearest.stationName.isNotBlank()) {
            if (nearest.statusType == "0") return "${nearest.stationName} 到站"
            val d = RouteCache.cached(city, cl.lineName, cl.direction)
            val prev = d?.data?.getOrNull(nearest.index - 1)
                ?.let { it.stationName.ifBlank { it.showName } }
            return prev?.takeIf { it.isNotBlank() } ?: nearest.stationName
        }
        val tip = res.routeOnStationRTimeInfoList.firstOrNull()
        return tip?.busToStationTips.orEmpty().ifBlank { cl.current }
    }

    private suspend fun fillStation(station: NearbyStation) {
        val lng = station.lon.toDoubleOrNull() ?: 0.0
        val lat = station.lat.toDoubleOrNull() ?: 0.0
        val raw = runCatching { api.linesByStation(city, station.name, lng, lat) }
            .getOrDefault(emptyList())
            .distinctBy { "${it.lineName}|${it.upperOrDown}" }
        val valid = pickValid(raw, station.name, lng, lat)
        val cards = valid.map { ln ->
            val dir = ln.upperOrDown.toIntOrNull() ?: 1
            val e = ends["${ln.lineName}|$dir"]
            val d = RouteCache.cached(city, ln.lineName, dir)
            CardLine(
                lineName = ln.lineName,
                direction = dir,
                from = ln.from.ifBlank { e?.first.orEmpty() }.ifBlank { RouteRows.firstStation(d) },
                to = ln.to.ifBlank { e?.second.orEmpty() }.ifBlank { RouteRows.lastStation(d) },
                stationOrder = ln.stationOrder,
                current = ln.neartext
            )
        }
        val firsts = valid.mapNotNull { ln ->
            RouteCache.cached(city, ln.lineName, ln.upperOrDown.toIntOrNull() ?: 1)?.beginTime
        }.mapNotNull { t -> if (t.contains(":")) t else null }
        val lasts = valid.mapNotNull { ln ->
            RouteCache.cached(city, ln.lineName, ln.upperOrDown.toIntOrNull() ?: 1)?.endTime
        }.mapNotNull { t -> if (t.contains(":")) t else null }
        val fl = if (firsts.isEmpty() || lasts.isEmpty()) null
        else firsts.min() to lasts.max()
        val list = _state.value.nearbyDetail.toMutableList()
        val i = list.indexOfFirst { it.station.name == station.name }
        if (i >= 0) {
            list[i] = list[i].copy(lines = cards, loading = false, firstLast = fl)
            _state.value = _state.value.copy(nearbyDetail = list)
        }
        updateCurrent(station, cards)
    }

    private suspend fun pickValid(
        candidates: List<BusLine>,
        stationName: String,
        lng: Double,
        lat: Double
    ): List<BusLine> = coroutineScope {
        val sem = Semaphore(4)
        val checks = candidates.take(10).map { ln ->
            async {
                sem.withPermit {
                    val dir = ln.upperOrDown.toIntOrNull() ?: 1
                    val d = runCatching { RouteCache.get(api, city, ln.lineName, dir) }.getOrNull()
                    RouteCache.matchesNear(d, stationName, lng, lat) to ln
                }
            }
        }
        checks.awaitAll().filter { it.first }.map { it.second }.take(3)
    }

    fun setFavorites(list: List<Favorite>) {
        _state.value = _state.value.copy(favorites = list)
    }

    fun showMessage(msg: String?) {
        _state.value = _state.value.copy(message = msg)
    }

    fun currentCity(): String = city
}

data class FavoriteCard(
    val favorite: Favorite,
    val current: String = "",
    val loading: Boolean = true
)

class FavoriteViewModel(private val api: BusApi = BusApi()) : ViewModel() {

    private val _cards = MutableStateFlow<List<FavoriteCard>>(emptyList())
    val cards: StateFlow<List<FavoriteCard>> = _cards.asStateFlow()

    private var job: Job? = null

    fun load(favorites: List<Favorite>, refresh: Boolean = false) {
        val same = _cards.value.map {
            "${it.favorite.city}|${it.favorite.line}|${it.favorite.direction}|${it.favorite.kind}"
        } == favorites.map { "${it.city}|${it.line}|${it.direction}|${it.kind}" }
        if (same && !refresh) return
        job?.cancel()
        _cards.value = favorites.map { FavoriteCard(it, loading = true) }
        job = viewModelScope.launch {
            favorites.forEach { fav -> launch { fill(fav) } }
        }
    }

    private suspend fun fill(fav: Favorite) {
        if (fav.kind == "metro") {
            val list0 = _cards.value.toMutableList()
            val i0 = list0.indexOfFirst {
                it.favorite.city == fav.city && it.favorite.line == fav.line &&
                    it.favorite.direction == fav.direction && it.favorite.kind == fav.kind
            }
            if (i0 >= 0) {
                list0[i0] = FavoriteCard(fav, "地铁", false)
                _cards.value = list0
            }
            return
        }
        val d = runCatching { RouteCache.get(api, fav.city, fav.line, fav.direction) }.getOrNull()
        val from = fav.from.ifBlank { RouteRows.firstStation(d) }
        val to = fav.to.ifBlank { RouteRows.lastStation(d) }
        val res = runCatching { api.realtime(fav.city, fav.line, fav.direction, 1) }.getOrNull()
        val nearest = res?.listBuses
            ?.filter { it.index >= 0 }
            ?.minByOrNull { it.index }
        val prevName = if (nearest != null && nearest.statusType != "0") {
            d?.data?.getOrNull(nearest.index - 1)?.let { it.stationName.ifBlank { it.showName } }
        } else null
        val cur = when {
            nearest == null -> res?.routeOnStationRTimeInfoList?.firstOrNull()?.busToStationTips.orEmpty()
            nearest.stationName.isBlank() -> ""
            nearest.statusType == "0" -> "${nearest.stationName} 到站"
            else -> prevName?.takeIf { it.isNotBlank() } ?: nearest.stationName
        }
        val list = _cards.value.toMutableList()
        val i = list.indexOfFirst {
            it.favorite.city == fav.city &&
                it.favorite.line == fav.line &&
                it.favorite.direction == fav.direction &&
                it.favorite.kind == fav.kind
        }
        if (i >= 0) {
            list[i] = FavoriteCard(fav.copy(from = from, to = to), cur, false)
            _cards.value = list
        }
    }
}

data class StationLinesState(
    val station: String = "",
    val lines: List<BusLine> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class StationLinesViewModel(private val api: BusApi = BusApi()) : ViewModel() {

    private val _state = MutableStateFlow(StationLinesState())
    val state: StateFlow<StationLinesState> = _state.asStateFlow()

    fun load(city: String, station: String, lng: Double, lat: Double) {
        _state.value = StationLinesState(station = station, loading = true)
        viewModelScope.launch {
            val raw = runCatching { api.linesByStation(city, station, lng, lat) }.getOrElse {
                _state.value = StationLinesState(station = station, loading = false, error = it.message)
                return@launch
            }
            val dedup = raw.distinctBy { "${it.lineName}|${it.upperOrDown}" }
            val sem = Semaphore(4)
            val res = coroutineScope {
                dedup.map { ln ->
                    async {
                        sem.withPermit {
                            val dir = ln.upperOrDown.toIntOrNull() ?: 1
                            val d = runCatching { RouteCache.get(api, city, ln.lineName, dir) }.getOrNull()
                            val keep = RouteCache.matchesNear(d, station, lng, lat)
                            keep to ln.copy(
                                from = ln.from.ifBlank { RouteRows.firstStation(d) },
                                to = ln.to.ifBlank { RouteRows.lastStation(d) }
                            )
                        }
                    }
                }.awaitAll().filter { it.first }.map { it.second }
            }
            _state.value = StationLinesState(
                station = station,
                lines = res,
                loading = false,
                error = if (res.isEmpty()) "该站点暂无线路" else null
            )
        }
    }
}

data class RouteUiState(
    val detail: RouteDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val buses: List<RealBus> = emptyList(),
    val speeds: List<SpeedItem> = emptyList(),
    val arrived: Set<String> = emptySet(),
    val upcoming: Set<String> = emptySet(),
    val lastName: String = "",
    val realtimeMsg: String = "",
    val direction: Int = 1
)

class RouteViewModel(private val api: BusApi = BusApi()) : ViewModel() {

    private val _state = MutableStateFlow(RouteUiState())
    val state: StateFlow<RouteUiState> = _state.asStateFlow()

    fun load(city: String, line: String, direction: Int) {
        val cached = RouteCache.cached(city, line, direction)
        _state.value = RouteUiState(
            detail = cached,
            loading = cached == null,
            direction = direction,
            lastName = RouteRows.lastStation(cached)
        )
        viewModelScope.launch {
            val d = runCatching { RouteCache.get(api, city, line, direction) }.getOrElse {
                _state.value = RouteUiState(loading = false, error = it.message ?: "加载失败")
                return@launch
            }
            if (d == null) {
                _state.value = RouteUiState(loading = false, error = "线路不存在", direction = direction)
            } else {
                _state.value = RouteUiState(
                    detail = d,
                    loading = false,
                    direction = direction,
                    lastName = RouteRows.lastStation(d)
                )
            }
        }
    }

    fun loadRealtime(city: String, line: String, direction: Int) {
        viewModelScope.launch {
            val res = runCatching { api.realtime(city, line, direction, 1) }.getOrNull() ?: return@launch
            val buses = res.listBuses
            val sameDir = direction == _state.value.direction
            if (buses.isEmpty() && _state.value.buses.isNotEmpty() && sameDir) {
                _state.value = _state.value.copy(realtimeMsg = res.msg)
                return@launch
            }
            _state.value = _state.value.copy(
                buses = buses,
                speeds = res.speedlist,
                arrived = RouteRows.arrivedStations(buses),
                upcoming = RouteRows.approachingStations(buses, _state.value.detail),
                realtimeMsg = res.msg,
                direction = direction
            )
        }
    }
}

package com.enddream.tracing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enddream.tracing.data.MetroLineRow
import com.enddream.tracing.data.MetroParser
import com.enddream.tracing.data.MetroStationRow
import com.enddream.tracing.data.MetroText
import com.enddream.tracing.data.MetroTime
import com.enddream.tracing.data.MetroTimetable
import com.enddream.tracing.data.NetTime
import com.enddream.tracing.data.api.BusApi
import com.enddream.tracing.data.api.MetroApi
import com.enddream.tracing.data.api.MetroCity
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class MetroNotice(
    val city: String,
    val shown: String
)

data class MetroUiState(
    val cities: List<MetroCity> = emptyList(),
    val city: MetroCity? = null,
    val lines: List<MetroLineRow> = emptyList(),
    val selectedLine: MetroLineRow? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val minuteOfDay: Int = 0,
    val notice: MetroNotice? = null,
    val times: Map<String, List<MetroTime>> = emptyMap(),
    val lineIdMap: Map<String, String> = emptyMap(),
    val reverse: Boolean = false,
    val noticeText: String? = null,
    val lineLength: Int = 0,
    val linePrice: Double = 0.0,
    val linePriceMin: Double = 0.0,
    val noticeLoading: Boolean = false,
    val firstLastFallback: Map<String, Pair<String, String>> = emptyMap(),
    val tick: Int = 0
)

class MetroViewModel(private val api: BusApi = BusApi()) : ViewModel() {

    private val _state = MutableStateFlow(MetroUiState())
    val state: StateFlow<MetroUiState> = _state.asStateFlow()

    private var spellMap: Map<String, String> = emptyMap()
    private var ticker: kotlinx.coroutines.Job? = null
    private var clockOffset: Long = 0L

    fun loadCities(autoCity: String? = null, auto: Boolean = true) {
        startTicker()
        if (_state.value.cities.isNotEmpty()) {
            if (auto && autoCity != null) pickForCity(_state.value.cities, autoCity)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val cities = api.metroCities()
            spellMap = api.metroSpellMap()
            _state.value = _state.value.copy(cities = cities, loading = false)
            if (auto && autoCity != null) pickForCity(cities, autoCity)
        }
    }

    var refreshSec: Int = 10

    private fun startTicker() {
        if (ticker != null) return
        ticker = viewModelScope.launch {
            clockOffset = com.enddream.tracing.data.offsetFromServer(NetTime.serverMillis())
            while (true) {
                val c = Calendar.getInstance()
                c.timeInMillis = System.currentTimeMillis() + clockOffset
                val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
                _state.value = _state.value.copy(
                    minuteOfDay = m,
                    tick = _state.value.tick + 1
                )
                delay(refreshSec.coerceAtLeast(3) * 1000L)
            }
        }
    }

    private fun pickForCity(cities: List<MetroCity>, cityName: String) {
        cities.firstOrNull { it.cityname == cityName }?.let {
            _state.value = _state.value.copy(notice = null)
            if (_state.value.city?.citycode != it.citycode) selectCity(it)
            return
        }
        val prov = provinceOf(cityName)
        val sameProv = if (prov != null) {
            com.enddream.tracing.data.MetroProvince.provinceCities[prov].orEmpty()
        } else emptyList()
        val cand = cities.filter { it.cityname in sameProv }
        val pick = cand.getOrNull(0) ?: cities.firstOrNull() ?: return
        if (!noticeOff) {
            _state.value = _state.value.copy(notice = MetroNotice(cityName, pick.cityname))
        }
        if (_state.value.city?.citycode != pick.citycode) selectCity(pick)
    }

    private fun provinceOf(cityName: String): String? {
        val raw = com.enddream.tracing.data.MetroProvince.cityProvince[cityName]
            ?: com.enddream.tracing.data.CityProvince.province[cityName]
            ?: return null
        val key = raw.removeSuffix("省").removeSuffix("市")
        return com.enddream.tracing.data.MetroProvince.provinceCities.keys
            .firstOrNull { it.removeSuffix("省").removeSuffix("市") == key }
            ?: raw
    }

    fun dismissNotice(forever: Boolean = false) {
        _state.value = _state.value.copy(notice = null)
        if (forever) noticeOff = true
    }

    var noticeOff: Boolean = false

    fun showCityList() {
        _state.value = _state.value.copy(
            city = null,
            lines = emptyList(),
            selectedLine = null,
            error = null,
            notice = null
        )
    }

    fun selectCity(city: MetroCity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                city = city,
                selectedLine = null,
                notice = null,
                times = emptyMap(),
                lineIdMap = emptyMap(),
                reverse = false,
                lineLength = 0,
                linePrice = 0.0,
                linePriceMin = 0.0,
                noticeText = null,
                firstLastFallback = emptyMap()
            )
            val spell = spellMap[city.adcode].orEmpty()
            if (spell.isBlank()) {
                _state.value = _state.value.copy(loading = false, error = "该城市暂无地铁线网数据")
                return@launch
            }
            val raw = api.metroNetwork(city.adcode, spell)
            if (raw.isNullOrBlank()) {
                _state.value = _state.value.copy(loading = false, error = "该城市暂无地铁线网数据")
                return@launch
            }
            val lines = MetroParser.parse(raw)
            val idMap = MetroParser.lineIdMap(raw)
            val infoRaw = api.metroInfo(city.adcode, spell)
            val times = if (infoRaw.isNullOrBlank()) emptyMap() else MetroParser.parseTimes(infoRaw)
            _state.value = _state.value.copy(
                lines = lines,
                lineIdMap = idMap,
                times = times,
                error = if (lines.isEmpty()) "该城市暂无地铁线网数据" else null
            )
            val filled = fillMissing(lines, times)
            _state.value = _state.value.copy(
                loading = false,
                times = filled.first,
                firstLastFallback = filled.second
            )
        }
    }

    fun reload() {
        val mc = _state.value.city ?: return
        selectCity(mc)
    }

    fun reloadDetail() {
        val mc = _state.value.city ?: return
        val cur = _state.value.selectedLine ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val spell = spellMap[mc.adcode].orEmpty()
            if (spell.isBlank()) {
                _state.value = _state.value.copy(loading = false)
                return@launch
            }
            val raw = api.metroNetwork(mc.adcode, spell)
            if (raw.isNullOrBlank()) {
                _state.value = _state.value.copy(loading = false)
                return@launch
            }
            val lines = MetroParser.parse(raw)
            val idMap = MetroParser.lineIdMap(raw)
            val infoRaw = api.metroInfo(mc.adcode, spell)
            val times = if (infoRaw.isNullOrBlank()) emptyMap() else MetroParser.parseTimes(infoRaw)
            val again = lines.firstOrNull { it.fullName == cur.fullName } ?: cur
            _state.value = _state.value.copy(
                lines = lines,
                lineIdMap = idMap,
                times = times,
                selectedLine = again
            )
            if (_state.value.firstLastFallback.isEmpty()) {
                val filled = fillMissing(lines, times)
                _state.value = _state.value.copy(
                    loading = false,
                    times = filled.first,
                    firstLastFallback = filled.second
                )
            }
            loadLineExt(again)
            loadNotice(again, if (_state.value.reverse) 2 else 1)
        }
    }

    fun refresh() {
        val mc = _state.value.city
        if (mc == null) return
        if (_state.value.lines.isEmpty()) {
            selectCity(mc)
            return
        }
        _state.value = _state.value.copy(tick = _state.value.tick + 1)
    }

    fun selectLineByName(name: String, reverse: Boolean) {
        val line = _state.value.lines.firstOrNull { it.fullName == name || it.name == name }
        if (line != null) {
            _state.value = _state.value.copy(
                selectedLine = line,
                reverse = reverse,
                lineLength = 0,
                linePrice = 0.0,
                noticeText = null
            )
            loadLineExt(line)
        }
    }

    fun selectLine(line: MetroLineRow) {
        _state.value = _state.value.copy(
            selectedLine = line,
            reverse = false,
            lineLength = 0,
            linePrice = 0.0,
            linePriceMin = 0.0,
            noticeText = null
        )
        loadLineExt(line)
    }

    fun toggleDirection() {
        _state.value = _state.value.copy(reverse = !_state.value.reverse)
    }

    fun backToLines() {
        _state.value = _state.value.copy(selectedLine = null, reverse = false)
    }

    fun backToCities() {
        _state.value = _state.value.copy(
            city = null,
            lines = emptyList(),
            selectedLine = null,
            error = null,
            notice = null,
            reverse = false
        )
    }

    fun loadNotice(line: MetroLineRow, direction: Int) {
        val head = line.stations.firstOrNull()
        val tail = line.stations.lastOrNull()
        viewModelScope.launch {
            _state.value = _state.value.copy(noticeLoading = true)
            var text = ""
            if (head != null && tail != null) {
                text = runCatching {
                    MetroApi().lineNotice(
                        head.lat, head.lng, head.poiid,
                        tail.lat, tail.lng, tail.poiid
                    )
                }.getOrDefault("")
            }
            if (text.isBlank()) {
                val names = listOf("地铁${line.name}", line.name).distinct()
                for (n in names) {
                    val d = runCatching { api.routeDetail(cityName(), n, direction) }.getOrNull()
                    val ann = MetroText.announcement(d?.commonts.orEmpty())
                    if (ann.isNotBlank()) {
                        text = ann
                        break
                    }
                }
                if (text.isBlank()) {
                    for (n in names) {
                        text = runCatching { api.lineNotice(cityName(), n, direction) }
                            .getOrDefault("")
                        if (text.isNotBlank()) break
                    }
                }
            }
            _state.value = _state.value.copy(noticeText = MetroText.dedupe(text), noticeLoading = false)
        }
    }

    fun loadLineExt(line: MetroLineRow) {
        val head = line.stations.firstOrNull() ?: return
        val tail = line.stations.lastOrNull() ?: return
        val second = line.stations.getOrNull(1)
        viewModelScope.launch {
            val api2 = MetroApi()
            val ext = runCatching {
                api2.lineExt(head.lat, head.lng, head.poiid, tail.lat, tail.lng, tail.poiid)
            }.getOrNull()
            val minExt = if (second != null) runCatching {
                api2.lineExt(head.lat, head.lng, head.poiid, second.lat, second.lng, second.poiid)
            }.getOrNull() else null
            _state.value = _state.value.copy(
                lineLength = ext?.first ?: 0,
                linePrice = ext?.second ?: 0.0,
                linePriceMin = minExt?.second ?: 0.0
            )
        }
    }

    private fun cityName(): String = _state.value.city?.cityname.orEmpty()

    fun lineFirstLast(line: MetroLineRow): Pair<String, String>? {
        val own = ownLineIds(line)
        val all = line.stations.flatMap { st ->
            _state.value.times[st.si].orEmpty()
                .filter { it.lineId.isEmpty() || own.isEmpty() || it.lineId in own }
        }
        return _state.value.firstLastFallback[line.fullName] ?: MetroTimetable.firstLastOf(all)
    }

    private fun ownTimesOf(line: MetroLineRow, st: MetroStationRow): List<MetroTime> {
        val own = ownLineIds(line)
        return _state.value.times[st.si].orEmpty()
            .filter { it.lineId.isEmpty() || own.isEmpty() || it.lineId in own }
    }

    private suspend fun fillMissing(
        lines: List<MetroLineRow>,
        times: Map<String, List<MetroTime>>
    ): Pair<Map<String, List<MetroTime>>, Map<String, Pair<String, String>>> {
        val merged = HashMap<String, List<MetroTime>>(times)
        val fallback = HashMap<String, Pair<String, String>>()
        val fetched = lines.map { line ->
            line to viewModelScope.async {
                var detail: com.enddream.tracing.data.api.RouteDetail? = null
                for (n in listOf("地铁${line.name}", line.name).distinct()) {
                    detail = runCatching { api.routeDetail(cityName(), n, 1) }.getOrNull()
                    if (detail?.routeName?.isNotBlank() == true) break
                }
                detail?.takeIf { it.routeName.isNotBlank() }
            }
        }.map { it.first to it.second.await() }
        for ((line, d) in fetched) {
            if (d == null) continue
            val own = ownLineIds(line)
            val lid = own.firstOrNull().orEmpty()
            val table = MetroText.stationTable(d.commonts)
            if (table == null) {
                d.firstLast.firstOrNull()?.let { fl ->
                    if (fl.first.isNotBlank() && fl.last.isNotBlank()) {
                        fallback[line.fullName] = fl.first to fl.last
                    }
                }
                continue
            }
            val tail = stationOf(line, table.directions.firstOrNull().orEmpty())
            val head = stationOf(line, table.directions.getOrNull(1).orEmpty())
            val firsts = ArrayList<String>()
            val lasts = ArrayList<String>()
            for (row in table.rows) {
                val st = stationOf(line, row.name) ?: continue
                val add = ArrayList<MetroTime>(2)
                val atTail = tail != null && st.si == tail.si
                val atHead = head != null && st.si == head.si
                if (!atTail && row.firstToward.first.isNotBlank() && row.firstToward.second.isNotBlank()) {
                    add.add(MetroTime(lid, row.firstToward.first, row.firstToward.second, tail?.si.orEmpty()))
                }
                if (!atHead && row.secondToward.first.isNotBlank() && row.secondToward.second.isNotBlank()) {
                    add.add(MetroTime(lid, row.secondToward.first, row.secondToward.second, head?.si.orEmpty()))
                }
                if (add.isEmpty()) continue
                merged[st.si] = merged[st.si].orEmpty().filter { it.lineId.isNotEmpty() && it.lineId !in own } + add
                if (!atTail) {
                    firsts.add(row.firstToward.first)
                    lasts.add(row.firstToward.second)
                }
                if (!atHead) {
                    firsts.add(row.secondToward.first)
                    lasts.add(row.secondToward.second)
                }
            }
            val first = firsts.mapNotNull { MetroTimetable.parseClock(it) }.filter { it < 24 * 60 }.minOrNull()
            val last = lasts.mapNotNull { MetroTimetable.parseClock(it) }.filter { it < 24 * 60 }.maxOrNull()
            if (first != null && last != null && last > first) {
                fallback[line.fullName] = MetroTimetable.formatClock(first) to MetroTimetable.formatClock(last)
            }
        }
        return merged to fallback
    }

    private fun stationOf(line: MetroLineRow, name: String): MetroStationRow? {
        val key = nameKey(name)
        if (key.isBlank()) return null
        return line.stations.firstOrNull { nameKey(it.name) == key }
    }

    private fun nameKey(text: String): String = text.filter { it.isLetterOrDigit() }

    private fun ownLineIds(line: MetroLineRow): Set<String> =
        _state.value.lineIdMap.entries.filter { it.value == line.fullName }
            .map { it.key }.toSet()

}

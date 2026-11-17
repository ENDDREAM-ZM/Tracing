package com.enddream.tracing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enddream.tracing.data.MetroLineRow
import com.enddream.tracing.data.MetroParser
import com.enddream.tracing.data.MetroTime
import com.enddream.tracing.data.MetroTimetable
import com.enddream.tracing.data.NetTime
import com.enddream.tracing.data.api.BusApi
import com.enddream.tracing.data.api.MetroCity
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
    val noticeText: String? = null
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

    private fun startTicker() {
        if (ticker != null) return
        ticker = viewModelScope.launch {
            clockOffset = com.enddream.tracing.data.offsetFromServer(NetTime.serverMillis())
            while (true) {
                val c = Calendar.getInstance()
                c.timeInMillis = System.currentTimeMillis() + clockOffset
                val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
                _state.value = _state.value.copy(minuteOfDay = m)
                delay(20_000L)
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
                reverse = false
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
                loading = false,
                lines = lines,
                lineIdMap = idMap,
                times = times,
                error = if (lines.isEmpty()) "该城市暂无地铁线网数据" else null
            )
        }
    }

    fun refresh() {
        val mc = _state.value.city
        if (mc != null && _state.value.lines.isEmpty()) selectCity(mc)
    }

    fun selectLineByName(name: String, reverse: Boolean) {
        val line = _state.value.lines.firstOrNull { it.fullName == name || it.name == name }
        if (line != null) {
            _state.value = _state.value.copy(selectedLine = line, reverse = reverse)
        }
    }

    fun selectLine(line: MetroLineRow) {
        _state.value = _state.value.copy(selectedLine = line, reverse = false)
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

    fun loadNotice(lineName: String, direction: Int) {
        if (lineName.isBlank()) return
        viewModelScope.launch {
            val text = runCatching { api.lineNotice(cityName(), lineName, direction) }
                .getOrDefault("")
            _state.value = _state.value.copy(noticeText = text)
        }
    }

    private fun cityName(): String = _state.value.city?.cityname.orEmpty()

    fun lineFirstLast(line: MetroLineRow): Pair<String, String>? {
        val all = line.stations.flatMap { _state.value.times[it.si].orEmpty() }
        return MetroTimetable.firstLastOf(all)
    }
}

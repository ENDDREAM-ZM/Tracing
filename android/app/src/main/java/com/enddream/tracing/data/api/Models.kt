package com.enddream.tracing.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Cmd {
    const val CITY_LIST = 101
    const val SEARCH_LINE = 102
    const val ROUTE_DETAIL = 103
    const val REALTIME = 104
    const val NEARBY = 106
    const val LINE_BY_STATION_V2 = 115
    const val SEARCH_LINE_ALL = 119
}

val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
}

@Serializable
data class CityItem(
    @SerialName("cityname") val name: String = ""
)

@Serializable
data class CityListResponse(
    val data: List<CityItem> = emptyList()
)

@Serializable
data class BusLine(
    val lineName: String = "",
    val from: String = "",
    val to: String = "",
    val upperOrDown: String = "",
    val beginTime: String = "",
    val endTime: String = "",
    val comments: String = "",
    val cityName: String = "",
    val stationName: String = "",
    val stationOrder: Int = 0,
    val neartext: String = "",
    val nexttext: String = "",
    val neardis: String = ""
)

@Serializable
data class LineSearchResponse(
    val buslines: List<BusLine> = emptyList(),
    val data: List<BusLine> = emptyList()
)

@Serializable
data class Station(
    val stationId: Int = 0,
    val stationName: String = "",
    val stationOrder: Int = 0,
    val showName: String = "",
    @SerialName("station_lon") val lon: Double = 0.0,
    @SerialName("station_lat") val lat: Double = 0.0,
    val come: Int = 0,
    val arrive: Int = 0,
    val stationsStatus: Int = 1,
    val sameNameNum: Int = 0
)

@Serializable
data class RouteDetail(
    val routeName: String = "",
    val upperOrDown: String = "",
    val beginTime: String = "",
    val endTime: String = "",
    val planTime: String = "",
    val routeColor: String = "",
    val commonts: String = "",
    val data: List<Station> = emptyList()
)

@Serializable
data class RealBus(
    val stationName: String = "",
    val index: Int = -1,
    val busNumber: String = "",
    val statusType: String = "",
    val busToStationNiheDistance: Double = 0.0,
    @SerialName("bus_lat") val busLat: Double = 0.0,
    @SerialName("bus_lng") val busLng: Double = 0.0
)

@Serializable
data class SpeedItem(
    val co: String = "",
    val speed: Double = 0.0
)

@Serializable
data class RouteOnStation(
    val routeName: String = "",
    val stationName: String = "",
    val stationIndex: String = "",
    val busToStationCount: Int = 0,
    val busToStationTime: Double = 0.0,
    val busToStationDistance: Double = 0.0,
    val busToStationTips: String = "",
    val busToStationName: String = "",
    val toStationName: String = "",
    val frmStationName: String = ""
)

@Serializable
data class RealtimeResponse(
    val routeOnStationRTimeInfoList: List<RouteOnStation> = emptyList(),
    val speedlist: List<SpeedItem> = emptyList(),
    val listBuses: List<RealBus> = emptyList(),
    val hasReal: Int = 0,
    val msg: String = ""
)

@Serializable
data class NearbyStation(
    val name: String = "",
    val lat: String = "",
    val lon: String = "",
    val dis: Int = 0,
    val sameNum: Int = 0
)

@Serializable
data class NearbyResponse(val data: List<NearbyStation> = emptyList())

@Serializable
data class MetroCity(
    val citycode: String = "",
    val cityname: String = "",
    val cityicon: String = "",
    val adcode: String = ""
)

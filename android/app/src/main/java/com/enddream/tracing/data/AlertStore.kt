package com.enddream.tracing.data

object AlertStore {

    private val armed = HashMap<String, String>()

    private fun key(line: String, dir: Int): String = "$line|$dir"

    fun get(line: String, dir: Int): String? = armed[key(line, dir)]

    fun set(line: String, dir: Int, station: String?) {
        val k = key(line, dir)
        if (station.isNullOrBlank()) armed.remove(k) else armed[k] = station
    }
}

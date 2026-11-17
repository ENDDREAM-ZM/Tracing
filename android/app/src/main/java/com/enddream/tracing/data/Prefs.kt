package com.enddream.tracing.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("xingji")

@Serializable
data class Favorite(
    val city: String,
    val line: String,
    val direction: Int,
    val from: String = "",
    val to: String = "",
    val kind: String = "bus"
)

object Prefs {

    private val KEY_CITY = stringPreferencesKey("city")
    private val KEY_FAV = stringPreferencesKey("favorites")
    private val KEY_REFRESH = intPreferencesKey("refresh_sec")
    private val KEY_NOTIFY = booleanPreferencesKey("notify_enabled")
    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_FLOAT_BAR = booleanPreferencesKey("float_bar")
    private val KEY_METRO_NOTICE = booleanPreferencesKey("metro_notice_off")
    private val KEY_CITY_SET = booleanPreferencesKey("city_set")
    private val KEY_RT_ORIENT = stringPreferencesKey("rt_orientation")

    private val json = Json { ignoreUnknownKeys = true }
    private val favSerializer = ListSerializer(Favorite.serializer())

    suspend fun city(ctx: Context): String =
        ctx.dataStore.data.first()[KEY_CITY].orEmpty().ifBlank { "厦门市" }

    suspend fun setCity(ctx: Context, city: String) {
        ctx.dataStore.edit {
            it[KEY_CITY] = city
            it[KEY_CITY_SET] = true
        }
    }

    suspend fun citySet(ctx: Context): Boolean =
        ctx.dataStore.data.first()[KEY_CITY_SET] ?: false

    fun favoritesFlow(ctx: Context): Flow<List<Favorite>> =
        ctx.dataStore.data.map { prefs -> decodeFavorites(prefs[KEY_FAV]) }

    suspend fun favorites(ctx: Context): List<Favorite> =
        decodeFavorites(ctx.dataStore.data.first()[KEY_FAV])

    private fun decodeFavorites(raw: String?): List<Favorite> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(favSerializer, raw) }.getOrDefault(emptyList())
    }

    suspend fun setFavorites(ctx: Context, list: List<Favorite>) {
        ctx.dataStore.edit { it[KEY_FAV] = json.encodeToString(favSerializer, list) }
    }

    suspend fun isFavorite(
        ctx: Context,
        city: String,
        line: String,
        direction: Int,
        kind: String = "bus"
    ): Boolean = favorites(ctx).any {
        it.city == city && it.line == line && it.direction == direction && it.kind == kind
    }

    suspend fun toggleFavorite(ctx: Context, fav: Favorite): Boolean {
        val list = favorites(ctx).toMutableList()
        val exist = list.indexOfFirst {
            it.city == fav.city && it.line == fav.line &&
                it.direction == fav.direction && it.kind == fav.kind
        }
        val added = if (exist >= 0) {
            list.removeAt(exist)
            false
        } else {
            list.add(fav)
            true
        }
        setFavorites(ctx, list)
        return added
    }

    suspend fun metroNoticeOff(ctx: Context): Boolean =
        ctx.dataStore.data.first()[KEY_METRO_NOTICE] ?: false

    suspend fun setMetroNoticeOff(ctx: Context, off: Boolean) {
        ctx.dataStore.edit { it[KEY_METRO_NOTICE] = off }
    }

    suspend fun refreshSec(ctx: Context): Int =
        ctx.dataStore.data.first()[KEY_REFRESH] ?: 10

    suspend fun setRefreshSec(ctx: Context, sec: Int) {
        ctx.dataStore.edit { it[KEY_REFRESH] = sec }
    }

    suspend fun themeMode(ctx: Context): String =
        ctx.dataStore.data.first()[KEY_THEME].orEmpty().ifBlank { "auto" }

    suspend fun setThemeMode(ctx: Context, mode: String) {
        ctx.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun floatBar(ctx: Context): Boolean =
        ctx.dataStore.data.first()[KEY_FLOAT_BAR] ?: true

    suspend fun setFloatBar(ctx: Context, on: Boolean) {
        ctx.dataStore.edit { it[KEY_FLOAT_BAR] = on }
    }

    suspend fun rtOrientation(ctx: Context): String =
        ctx.dataStore.data.first()[KEY_RT_ORIENT].orEmpty().ifBlank { "h" }

    suspend fun setRtOrientation(ctx: Context, v: String) {
        ctx.dataStore.edit { it[KEY_RT_ORIENT] = v }
    }

    suspend fun notifyEnabled(ctx: Context): Boolean =
        ctx.dataStore.data.first()[KEY_NOTIFY] ?: false

    suspend fun setNotifyEnabled(ctx: Context, on: Boolean) {
        ctx.dataStore.edit { it[KEY_NOTIFY] = on }
    }
}

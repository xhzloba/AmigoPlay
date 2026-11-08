package com.example.movie.data.alerts

import android.content.Context
import com.example.movie.domain.model.SeriesAlert
import com.example.movie.domain.model.SeriesSub
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReleaseWatchStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val subsState = MutableStateFlow(read(KEY_SUBS, emptyList<SeriesSub>()))
    private val alertsState = MutableStateFlow(read(KEY_ALERTS, emptyList<SeriesAlert>()))
    private val idsState = MutableStateFlow(subsState.value.map { it.id }.toSet())

    val subs: StateFlow<List<SeriesSub>> = subsState.asStateFlow()
    val alerts: StateFlow<List<SeriesAlert>> = alertsState.asStateFlow()
    val ids: StateFlow<Set<String>> = idsState.asStateFlow()

    fun arm(sub: SeriesSub) {
        if (sub.id.isBlank()) return
        writeSubs(listOf(sub) + subsState.value.filterNot { it.id == sub.id })
    }

    fun disarm(id: String) {
        if (!idsState.value.contains(id)) return
        writeSubs(subsState.value.filterNot { it.id == id })
    }

    fun markFeed(id: String, seen: Boolean) {
        val current = subsState.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || current[index].feedSeen == seen) return
        val next = current.toMutableList()
        next[index] = current[index].copy(feedSeen = seen)
        writeSubs(next)
    }

    fun baseline(id: String, airedKey: String, nextEpochDay: Long?) {
        val current = subsState.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val next = current.toMutableList()
        next[index] = current[index].copy(airedKey = airedKey, nextEpochDay = nextEpochDay)
        writeSubs(next)
    }

    fun absorbFeed(movieId: String, season: Int, episode: Int): Boolean {
        val current = alertsState.value
        val latest = current.firstOrNull { it.movieId == movieId } ?: return false
        if (!latest.feed) return false
        val next = current.toMutableList()
        next[current.indexOf(latest)] = latest.copy(season = season, episode = episode, feed = false)
        writeAlerts(next)
        return true
    }

    fun push(alert: SeriesAlert): Boolean {
        if (alertsState.value.any { it.id == alert.id }) return false
        writeAlerts((listOf(alert) + alertsState.value).take(40))
        return true
    }

    fun markMovie(movieId: String) {
        if (alertsState.value.none { it.movieId == movieId && !it.read }) return
        writeAlerts(alertsState.value.map { if (it.movieId == movieId) it.copy(read = true) else it })
    }

    private fun writeSubs(next: List<SeriesSub>) {
        prefs.edit().putString(KEY_SUBS, json.encodeToString(next)).commit()
        subsState.value = next
        idsState.value = next.map { it.id }.toSet()
    }

    private fun writeAlerts(next: List<SeriesAlert>) {
        prefs.edit().putString(KEY_ALERTS, json.encodeToString(next)).commit()
        alertsState.value = next
    }

    private inline fun <reified T> read(key: String, empty: List<T>): List<T> {
        val raw = prefs.getString(key, null) ?: return empty
        return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(empty)
    }

    private companion object {
        const val FILE = "release_watch"
        const val KEY_SUBS = "subs"
        const val KEY_ALERTS = "alerts"
    }
}

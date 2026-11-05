package com.example.movie.data.history

import android.content.Context
import com.example.movie.domain.model.SavedTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ViewHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsState = MutableStateFlow(read())

    val items: StateFlow<List<SavedTitle>> = itemsState.asStateFlow()

    fun remember(item: SavedTitle) {
        if (item.id.isBlank() || item.posterUrl.isNullOrBlank()) return
        val previous = itemsState.value.firstOrNull { it.id == item.id }
        val stored = if (item.logoUrl.isNullOrBlank()) item.copy(logoUrl = previous?.logoUrl) else item
        val next = (listOf(stored) + itemsState.value.filter { it.id != item.id && !it.posterUrl.isNullOrBlank() }).take(CAP)
        write(next)
    }

    fun clear() {
        write(emptyList())
    }

    fun attachLogo(id: String, logoUrl: String) {
        if (id.isBlank() || logoUrl.isBlank()) return
        val current = itemsState.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || current[index].logoUrl == logoUrl) return
        val next = current.toMutableList()
        next[index] = current[index].copy(logoUrl = logoUrl)
        write(next)
    }

    private fun read(): List<SavedTitle> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val decoded = runCatching { json.decodeFromString<List<SavedTitle>>(raw) }.getOrDefault(emptyList())
        val clean = decoded.filter { it.id.isNotBlank() && !it.posterUrl.isNullOrBlank() }
        if (clean.size != decoded.size) {
            prefs.edit().putString(KEY, json.encodeToString(clean)).commit()
        }
        return clean
    }

    private fun write(next: List<SavedTitle>) {
        prefs.edit().putString(KEY, json.encodeToString(next)).commit()
        itemsState.value = next
    }

    private companion object {
        const val FILE = "view_history"
        const val KEY = "items"
        const val CAP = 40
    }
}

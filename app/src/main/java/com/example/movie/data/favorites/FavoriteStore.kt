package com.example.movie.data.favorites

import android.content.Context
import com.example.movie.domain.model.SavedTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FavoriteStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsState = MutableStateFlow(read())
    private val idsState = MutableStateFlow(itemsState.value.map { it.id }.toSet())

    val items: StateFlow<List<SavedTitle>> = itemsState.asStateFlow()
    val ids: StateFlow<Set<String>> = idsState.asStateFlow()

    fun toggle(item: SavedTitle) {
        if (item.id.isBlank()) return
        val next = if (idsState.value.contains(item.id)) {
            itemsState.value.filterNot { it.id == item.id }
        } else {
            listOf(item) + itemsState.value
        }
        prefs.edit().putString(KEY, json.encodeToString(next)).commit()
        itemsState.value = next
        idsState.value = next.map { it.id }.toSet()
    }

    private fun read(): List<SavedTitle> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SavedTitle>>(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        const val FILE = "favorites"
        const val KEY = "items"
    }
}

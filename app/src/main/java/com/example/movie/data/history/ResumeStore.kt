package com.example.movie.data.history

import android.content.Context
import com.example.movie.domain.model.ResumeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal enum class ResumeWrite { Save, Drop, Ignore }

internal fun resumeWrite(positionMs: Long, durationMs: Long, sameEpisode: Boolean): ResumeWrite {
    val finished = durationMs > 0L &&
        (durationMs - positionMs < FINISH_TAIL_MS || positionMs >= durationMs * 95 / 100)
    if (finished) return ResumeWrite.Drop
    if (positionMs < MIN_POSITION_MS && sameEpisode) return ResumeWrite.Ignore
    return ResumeWrite.Save
}

class ResumeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsState = MutableStateFlow(read())

    val items: StateFlow<List<ResumeItem>> = itemsState.asStateFlow()

    fun record(item: ResumeItem) {
        if (item.id.isBlank() || item.franchiseId <= 0 || item.posterUrl.isNullOrBlank()) return
        val current = itemsState.value
        val previous = current.firstOrNull { it.id == item.id }
        val sameEpisode = previous == null || (previous.season == item.season && previous.episode == item.episode)
        when (resumeWrite(item.positionMs, item.durationMs, sameEpisode)) {
            ResumeWrite.Ignore -> return
            ResumeWrite.Drop -> write(current.filter { it.id != item.id })
            ResumeWrite.Save -> {
                val stored = item.copy(
                    posterUrl = item.posterUrl ?: previous?.posterUrl,
                    logoUrl = if (item.logoUrl.isNullOrBlank()) previous?.logoUrl else item.logoUrl,
                    genres = item.genres.ifBlank { previous?.genres.orEmpty() },
                    year = item.year ?: previous?.year,
                    updatedAt = System.currentTimeMillis(),
                )
                write((listOf(stored) + current.filter { it.id != item.id }).take(CAP))
            }
        }
    }

    private fun read(): List<ResumeItem> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ResumeItem>>(raw) }.getOrDefault(emptyList())
            .filter { it.id.isNotBlank() && it.franchiseId > 0 && !it.posterUrl.isNullOrBlank() }
    }

    private fun write(next: List<ResumeItem>) {
        prefs.edit().putString(KEY, json.encodeToString(next)).commit()
        itemsState.value = next
    }

    private companion object {
        const val FILE = "resume"
        const val KEY = "items"
        const val CAP = 40
    }
}

private const val MIN_POSITION_MS = 10_000L
private const val FINISH_TAIL_MS = 20_000L

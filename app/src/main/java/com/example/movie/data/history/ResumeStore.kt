package com.example.movie.data.history

import android.content.Context
import com.example.movie.domain.model.EpisodeProgress
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.episodeFinished
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal enum class ResumeWrite { Save, Complete, Drop, Ignore }

internal fun resumeWrite(positionMs: Long, durationMs: Long, series: Boolean): ResumeWrite {
    if (episodeFinished(positionMs, durationMs)) return if (series) ResumeWrite.Complete else ResumeWrite.Drop
    if (positionMs < MIN_POSITION_MS) return ResumeWrite.Ignore
    return ResumeWrite.Save
}

class ResumeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsState = MutableStateFlow(read())

    val items: StateFlow<List<ResumeItem>> = itemsState.asStateFlow()

    fun record(item: ResumeItem, seriesDone: (ResumeItem) -> Boolean = { false }): Boolean {
        if (item.id.isBlank() || item.franchiseId <= 0 || item.posterUrl.isNullOrBlank()) return false
        val current = itemsState.value
        val previous = current.firstOrNull { it.id == item.id }
        val series = item.season != null && item.episode != null
        when (resumeWrite(item.positionMs, item.durationMs, series)) {
            ResumeWrite.Ignore -> return false
            ResumeWrite.Drop -> {
                write(current.filter { it.id != item.id })
                return true
            }
            ResumeWrite.Save, ResumeWrite.Complete -> {
                val completed = episodeFinished(item.positionMs, item.durationMs)
                val stored = item.copy(
                    posterUrl = item.posterUrl ?: previous?.posterUrl,
                    logoUrl = if (item.logoUrl.isNullOrBlank()) previous?.logoUrl else item.logoUrl,
                    genres = item.genres.ifBlank { previous?.genres.orEmpty() },
                    year = item.year ?: previous?.year,
                    positionMs = if (completed) item.durationMs else item.positionMs,
                    marks = mergeMarks(previous?.marks.orEmpty(), item, completed),
                    updatedAt = System.currentTimeMillis(),
                )
                val finished = seriesDone(stored)
                val rest = current.filter { it.id != item.id }
                write(if (finished) rest else (listOf(stored) + rest).take(CAP))
                return finished
            }
        }
    }

    fun forget(id: String) {
        if (id.isBlank()) return
        write(itemsState.value.filter { it.id != id })
    }

    private fun mergeMarks(previous: List<EpisodeProgress>, item: ResumeItem, completed: Boolean): List<EpisodeProgress> {
        val season = item.season ?: return previous
        val episode = item.episode ?: return previous
        val next = EpisodeProgress(
            season = season,
            episode = episode,
            positionMs = if (completed) item.durationMs else item.positionMs,
            durationMs = item.durationMs,
            completed = completed,
        )
        return listOf(next) + previous.filterNot { it.season == season && it.episode == episode }
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

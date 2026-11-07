package com.example.movie.data.history

import android.content.Context
import com.example.movie.domain.model.EpisodeProgress
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.episodeFinished
import com.example.movie.domain.model.combineMarks
import com.example.movie.domain.model.episodeMarks
import com.example.movie.domain.model.fullyWatched
import com.example.movie.domain.model.isUnreleased
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
    private val keptState = MutableStateFlow(readKept())

    val items: StateFlow<List<ResumeItem>> = itemsState.asStateFlow()
    val kept: StateFlow<Map<String, List<EpisodeProgress>>> = keptState.asStateFlow()

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
                    overview = item.overview.ifBlank { previous?.overview.orEmpty() },
                    ratingKp = item.ratingKp ?: previous?.ratingKp,
                    ratingImdb = item.ratingImdb ?: previous?.ratingImdb,
                    year = item.year ?: previous?.year,
                    positionMs = if (completed) item.durationMs else item.positionMs,
                    marks = mergeMarks(
                        combineMarks(previous?.marks.orEmpty(), keptState.value[item.id].orEmpty()),
                        item,
                        completed,
                    ),
                    updatedAt = System.currentTimeMillis(),
                )
                val finished = seriesDone(stored)
                val rest = current.filter { it.id != item.id }
                if (finished) keepMarks(stored.id, stored.episodeMarks())
                write(if (finished) rest else (listOf(stored) + rest).take(CAP))
                return finished
            }
        }
    }

    fun fillOverview(id: String, overview: String) {
        val text = overview.trim()
        if (id.isBlank() || text.isEmpty()) return
        val current = itemsState.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || current[index].overview.isNotBlank()) return
        write(current.toMutableList().also { it[index] = it[index].copy(overview = text) })
    }

    fun fillRatings(id: String, ratingKp: Float?, ratingImdb: Float?) {
        if (id.isBlank() || (ratingKp == null && ratingImdb == null)) return
        val current = itemsState.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val item = current[index]
        if (item.ratingKp != null || item.ratingImdb != null) return
        write(current.toMutableList().also { it[index] = item.copy(ratingKp = ratingKp, ratingImdb = ratingImdb) })
    }

    fun forget(id: String) {
        if (id.isBlank()) return
        write(itemsState.value.filter { it.id != id })
    }

    fun clearMarks(id: String) {
        if (id.isBlank() || id !in keptState.value) return
        writeKept(keptState.value - id)
    }

    fun resetProgress(id: String) {
        if (id.isBlank()) return
        clearMarks(id)
        forget(id)
    }

    fun watchSeasons(id: String, seasons: List<FranchiseSeason>, selected: Set<Int>): Boolean {
        if (id.isBlank() || selected.isEmpty() || seasons.isEmpty()) return false
        val current = itemsState.value
        val previous = current.firstOrNull { it.id == id }
        val base = combineMarks(previous?.episodeMarks().orEmpty(), keptState.value[id].orEmpty())
        val merged = applySelection(base, seasons, selected)
        val pickedAll = seasons.all { it.number in selected }
        val done = pickedAll || seasons.fullyWatched(merged)
        keepMarks(id, merged)
        if (previous == null) return done
        if (done) {
            write(current.filter { it.id != id })
            return true
        }
        val seasonReset = previous.season != null && previous.season !in selected
        val hereDone = previous.season != null && previous.episode != null &&
            merged.any { it.completed && it.season == previous.season && it.episode == previous.episode }
        val next = if (seasonReset || hereDone) nextOpen(seasons, merged) else null
        val updated = if (next == null) {
            previous.copy(marks = merged, updatedAt = System.currentTimeMillis())
        } else {
            previous.copy(
                marks = merged,
                season = next.first,
                episode = next.second,
                positionMs = 0L,
                durationMs = 0L,
                updatedAt = System.currentTimeMillis(),
            )
        }
        write(current.map { if (it.id == id) updated else it })
        return false
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

    private fun keepMarks(id: String, marks: List<EpisodeProgress>) {
        if (id.isBlank()) return
        writeKept(if (marks.isEmpty()) keptState.value - id else keptState.value + (id to marks))
    }

    private fun write(next: List<ResumeItem>) {
        prefs.edit().putString(KEY, json.encodeToString(next)).commit()
        itemsState.value = next
    }

    private fun writeKept(next: Map<String, List<EpisodeProgress>>) {
        val packed = next.map { (id, marks) -> KeptProgress(id, marks) }
        prefs.edit().putString(MARKS, json.encodeToString(packed)).commit()
        keptState.value = next
    }

    private fun readKept(): Map<String, List<EpisodeProgress>> {
        val raw = prefs.getString(MARKS, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<List<KeptProgress>>(raw) }
            .getOrDefault(emptyList())
            .associate { it.id to it.marks }
    }

    private companion object {
        const val FILE = "resume"
        const val KEY = "items"
        const val MARKS = "marks"
        const val CAP = 40
    }
}

@kotlinx.serialization.Serializable
private data class KeptProgress(val id: String, val marks: List<EpisodeProgress>)

private fun applySelection(
    existing: List<EpisodeProgress>,
    seasons: List<FranchiseSeason>,
    selected: Set<Int>,
): List<EpisodeProgress> {
    val drop = seasons.filter { it.number !in selected }
        .flatMap { season -> season.episodes.map { season.number to it.number } }
        .toSet()
    return stamp(existing.filterNot { (it.season to it.episode) in drop }, seasons, selected)
}

private fun stamp(
    existing: List<EpisodeProgress>,
    seasons: List<FranchiseSeason>,
    selected: Set<Int>,
): List<EpisodeProgress> {
    val stamped = seasons.filter { it.number in selected }.flatMap { season ->
        season.episodes.filterNot { it.release.isUnreleased() }.map { episode ->
            EpisodeProgress(
                season = season.number,
                episode = episode.number,
                positionMs = 1L,
                durationMs = 1L,
                completed = true,
            )
        }
    }
    if (stamped.isEmpty()) return existing
    val keys = stamped.map { it.season to it.episode }.toSet()
    return stamped + existing.filterNot { (it.season to it.episode) in keys }
}

private fun nextOpen(seasons: List<FranchiseSeason>, marks: List<EpisodeProgress>): Pair<Int, Int>? {
    for (season in seasons) {
        for (episode in season.episodes) {
            if (episode.release.isUnreleased()) continue
            val done = marks.any { it.completed && it.season == season.number && it.episode == episode.number }
            if (!done) return season.number to episode.number
        }
    }
    return null
}

private const val MIN_POSITION_MS = 10_000L

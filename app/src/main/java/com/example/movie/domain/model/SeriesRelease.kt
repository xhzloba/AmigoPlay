package com.example.movie.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class SeriesSub(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val airedKey: String = "",
    val nextEpochDay: Long? = null,
    val feedSeen: Boolean = true,
)

@Serializable
data class SeriesAlert(
    val id: String,
    val movieId: String,
    val title: String,
    val posterUrl: String? = null,
    val season: Int,
    val episode: Int,
    val at: Long,
    val read: Boolean = false,
    val feed: Boolean = false,
)

internal data class AiredEpisode(val season: Int, val episode: Int) : Comparable<AiredEpisode> {
    override fun compareTo(other: AiredEpisode): Int =
        compareValuesBy(this, other, { it.season }, { it.episode })

    fun key(): String = "$season:$episode"
}

internal data class ReleaseSnapshot(
    val key: String,
    val aired: AiredEpisode?,
    val nextEpochDay: Long?,
    val undated: Boolean,
)

internal fun Franchise.releaseSnapshot(today: LocalDate = LocalDate.now()): ReleaseSnapshot {
    var aired: AiredEpisode? = null
    var next: LocalDate? = null
    var dated = false
    var any = false
    for (season in seasons) {
        for (episode in season.episodes) {
            any = true
            val date = episode.release.toLocalDate()
            if (date != null) dated = true
            val future = date != null && date.isAfter(today)
            if (!future) {
                val candidate = AiredEpisode(season.number, episode.number)
                if (aired == null || candidate > aired) aired = candidate
            } else if (next == null || date.isBefore(next)) {
                next = date
            }
        }
    }
    return ReleaseSnapshot(
        key = aired?.key() ?: "none",
        aired = aired,
        nextEpochDay = next?.toEpochDay(),
        undated = any && !dated,
    )
}

internal fun shouldAlert(previousKey: String, snapshot: ReleaseSnapshot): AiredEpisode? {
    if (previousKey.isEmpty()) return null
    val next = snapshot.aired ?: return null
    val previous = previousKey.toAired()
    return if (previous == null || next > previous) next else null
}

private fun String.toAired(): AiredEpisode? {
    val parts = split(':')
    if (parts.size != 2) return null
    val season = parts[0].toIntOrNull() ?: return null
    val episode = parts[1].toIntOrNull() ?: return null
    if (season <= 0 || episode <= 0) return null
    return AiredEpisode(season, episode)
}

private fun String?.toLocalDate(): LocalDate? {
    val raw = this?.trim().orEmpty()
    if (raw.length < 10) return null
    return runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
}

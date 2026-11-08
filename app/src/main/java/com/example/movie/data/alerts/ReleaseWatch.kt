package com.example.movie.data.alerts

import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.SeriesAlert
import com.example.movie.domain.model.releaseSnapshot
import com.example.movie.domain.model.shouldAlert
import com.example.movie.domain.repository.MovieRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class ReleaseWatch(
    val store: ReleaseWatchStore,
    private val movies: MovieRepository,
    private val notify: (SeriesAlert) -> Unit,
    private val foreground: () -> Boolean,
) {
    private val gate = Mutex()

    suspend fun refresh() {
        if (store.subs.value.isEmpty()) return
        gate.withLock {
            val today = LocalDate.now()
            val epoch = today.toEpochDay()
            val feed = movies.getMovies(CatalogQuery(sort = "updatings"), page = 1)
                .getOrNull()
                ?.map { it.id }
                ?.toSet()
            val subs = store.subs.value
            for (sub in subs) {
                val dateDue = sub.nextEpochDay != null && sub.nextEpochDay <= epoch
                val inFeed = feed?.contains(sub.id) == true
                val unseeded = sub.airedKey.isEmpty()
                if (feed != null && !inFeed && sub.feedSeen) store.markFeed(sub.id, false)
                if (!dateDue && !inFeed && !unseeded) continue
                val franchise = movies.franchise(sub.id) ?: continue
                val snapshot = franchise.releaseSnapshot(today)
                val aired = shouldAlert(sub.airedKey, snapshot)
                if (aired != null) {
                    val absorbed = store.absorbFeed(sub.id, aired.season, aired.episode)
                    if (!absorbed) {
                        val alert = SeriesAlert(
                            id = "${sub.id}:${aired.key()}",
                            movieId = sub.id,
                            title = sub.title,
                            posterUrl = sub.posterUrl,
                            season = aired.season,
                            episode = aired.episode,
                            at = System.currentTimeMillis(),
                        )
                        if (store.push(alert) && !foreground()) notify(alert)
                    }
                    if (inFeed) store.markFeed(sub.id, true)
                }
                val feedUpdate = snapshot.undated && inFeed && !sub.feedSeen && !unseeded && aired == null
                if (feedUpdate) {
                    val alert = SeriesAlert(
                        id = "${sub.id}:feed:${System.currentTimeMillis()}",
                        movieId = sub.id,
                        title = sub.title,
                        posterUrl = sub.posterUrl,
                        season = 0,
                        episode = 0,
                        at = System.currentTimeMillis(),
                        feed = true,
                    )
                    if (store.push(alert) && !foreground()) notify(alert)
                    store.markFeed(sub.id, true)
                }
                val keep = snapshot.aired == null && sub.airedKey.isNotEmpty() && sub.airedKey != "none"
                store.baseline(
                    sub.id,
                    if (keep) sub.airedKey else snapshot.key,
                    snapshot.nextEpochDay,
                )
            }
        }
    }
}

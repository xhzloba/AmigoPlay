package com.example.movie.data.alerts

import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.FeedCard
import com.example.movie.domain.model.FeedKind
import com.example.movie.domain.model.FeedShelf
import com.example.movie.domain.model.SeriesAlert
import com.example.movie.domain.model.SeriesSub
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
    private val notifyShelf: (kind: String, count: Int) -> Unit,
    private val foreground: () -> Boolean,
) {
    private val gate = Mutex()

    suspend fun refresh() {
        store.purge(System.currentTimeMillis())
        if (store.subs.value.isEmpty() && !store.feed.value.anyOn) return
        gate.withLock {
            val held = mutableSetOf<String>()
            if (store.subs.value.isNotEmpty()) refreshSeries(held)
            if (store.feed.value.anyOn) refreshFeeds(held)
        }
    }

    private suspend fun refreshSeries(held: MutableSet<String>) {
        val today = LocalDate.now()
        val epoch = today.toEpochDay()
        val feed = movies.getMovies(CatalogQuery(sort = "updatings"), page = 1)
            .getOrNull()
            ?.map { it.id }
            ?.toSet()
        val subs = store.subs.value
        val waiting = subs.any { it.awaitingPlayer }
        val updates = if (waiting) shelfIds("updatings") else null
        val fresh = if (waiting) shelfIds("new") else null
        for (sub in subs) {
            if (sub.awaitingPlayer) {
                awaitPlayer(sub, updates, fresh, held)
                continue
            }
            val dateDue = sub.nextEpochDay != null && sub.nextEpochDay <= epoch
            val inFeed = feed?.contains(sub.id) == true
            val unseeded = sub.airedKey.isEmpty()
            if (feed != null && !inFeed && sub.feedSeen) store.markFeed(sub.id, false)
            if (!dateDue && !inFeed && !unseeded) continue
            val franchise = movies.franchise(sub.id) ?: continue
            val snapshot = franchise.releaseSnapshot(today)
            val aired = shouldAlert(sub.airedKey, snapshot)
            var told = false
            if (aired != null) {
                val absorbed = store.absorbFeed(sub.id, aired.season, aired.episode)
                val alert = if (absorbed) {
                    store.alerts.value.firstOrNull {
                        it.movieId == sub.id && it.season == aired.season && it.episode == aired.episode
                    }
                } else {
                    SeriesAlert(
                        id = "${sub.id}:${aired.key()}",
                        movieId = sub.id,
                        title = sub.title,
                        posterUrl = sub.posterUrl,
                        season = aired.season,
                        episode = aired.episode,
                        at = System.currentTimeMillis(),
                    ).takeIf { store.push(it) }
                }
                if (alert != null && !foreground()) notify(alert)
                if (inFeed) store.markFeed(sub.id, true)
                told = true
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
                told = true
            }
            val finished = franchise.series && franchise.serialStatus.equals("offline", ignoreCase = true)
            if (told || finished) {
                store.disarm(sub.id)
                held.add(sub.id)
                continue
            }
            val keep = snapshot.aired == null && sub.airedKey.isNotEmpty() && sub.airedKey != "none"
            store.baseline(
                sub.id,
                if (keep) sub.airedKey else snapshot.key,
                snapshot.nextEpochDay,
            )
        }
    }

    private suspend fun awaitPlayer(
        sub: SeriesSub,
        updates: Set<String>?,
        fresh: Set<String>?,
        held: MutableSet<String>,
    ) {
        val franchise = movies.franchise(sub.id)
        val listed = updates?.contains(sub.id) == true || fresh?.contains(sub.id) == true
        if (franchise?.watchId != null) {
            val alert = SeriesAlert(
                id = "${sub.id}:player",
                movieId = sub.id,
                title = sub.title,
                posterUrl = sub.posterUrl,
                season = 0,
                episode = 0,
                at = System.currentTimeMillis(),
                available = true,
            )
            if (store.push(alert) && !foreground()) notify(alert)
            store.disarm(sub.id)
            held.add(sub.id)
            return
        }
        if (updates == null && fresh == null) return
        if (sub.airedKey == "pending") {
            store.markFeed(sub.id, listed)
            store.baseline(sub.id, "seeded", null)
            return
        }
        if (!listed || sub.feedSeen) return
        val alert = SeriesAlert(
            id = "${sub.id}:listed",
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

    private suspend fun shelfIds(sort: String): Set<String>? {
        val movie = movies.getMovies(CatalogQuery(sort = sort, type = "movie"), page = 1).getOrNull()
        val serial = movies.getMovies(CatalogQuery(sort = sort, type = "serial"), page = 1).getOrNull()
        if (movie == null && serial == null) return null
        return (movie.orEmpty() + serial.orEmpty()).map { it.id }.toSet()
    }

    private suspend fun refreshFeeds(held: Set<String>) {
        val prefs = store.feed.value
        val now = System.currentTimeMillis()
        val hours = prefs.intervalHours.takeIf { it in FeedKind.intervals } ?: 6
        val interval = hours * 60L * 60L * 1000L
        val belled = store.subs.value.map { it.id }.toSet() + held
        for (kind in FeedKind.all) {
            if (!prefs.enabled(kind)) continue
            val current = store.shelves.value.firstOrNull { it.kind == kind } ?: FeedShelf(kind)
            val due = current.polledAt == 0L || now - current.polledAt >= interval
            if (current.seen.isNotEmpty() && !due) {
                if (current.offAt != null) store.replaceShelf(current.copy(offAt = null))
                continue
            }
            val page = movies.getMovies(
                CatalogQuery(sort = FeedKind.sort(kind), type = FeedKind.type(kind)),
                page = 1,
            ).getOrNull() ?: continue
            if (!store.feed.value.enabled(kind)) continue
            val seen = current.seen.toSet()
            val seeded = current.seen.isEmpty()
            val fresh = page.filter { it.id !in seen && it.id !in belled }
            val nextSeen = (page.map { it.id } + current.seen).distinct().take(500)
            if (seeded || fresh.isEmpty()) {
                store.replaceShelf(current.copy(seen = nextSeen, polledAt = now, offAt = null))
                continue
            }
            val cards = fresh.map { movie ->
                FeedCard(
                    id = movie.id,
                    title = movie.title,
                    posterUrl = movie.posterUrl,
                    year = movie.year,
                    type = movie.type.ifBlank { FeedKind.type(kind) },
                    originalTitle = movie.originalTitle,
                    ratingKp = movie.ratingKp,
                    ratingImdb = movie.ratingImdb,
                    age = movie.age,
                )
            }
            store.replaceShelf(
                current.copy(
                    cards = cards,
                    seen = nextSeen,
                    at = now,
                    offAt = null,
                    read = false,
                    polledAt = now,
                ),
            )
            if (!foreground()) notifyShelf(kind, cards.size)
        }
    }
}

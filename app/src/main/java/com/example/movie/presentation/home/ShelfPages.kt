package com.example.movie.presentation.home

import com.example.movie.domain.model.Movie

internal data class ShelfTake(
    val fresh: List<Movie>,
    val end: Boolean,
    val again: Boolean,
)

/**
 * Конец ленты — это пустой ответ, отсутствие `page.next` или точная копия предыдущей страницы.
 * Сдвиг, где все id уже были (сериал снова всплыл с новой серией), не конец: берём следующую.
 */
internal const val ShelfOverlapSkips = 6

internal class ShelfPages {
    private val seen = mutableSetOf<String>()
    private var lastIds: Set<String> = emptySet()
    var page: Int = 0
        private set

    fun rewind() {
        page = 0
    }

    fun reset() {
        seen.clear()
        lastIds = emptySet()
        page = 0
    }

    fun advance(batch: List<Movie>, hasMore: Boolean, paged: Boolean): ShelfTake {
        val ids = LinkedHashSet<String>(batch.size)
        val fresh = ArrayList<Movie>(batch.size)
        for (movie in batch) {
            if (!ids.add(movie.id)) continue
            if (seen.add(movie.id)) fresh.add(movie)
        }
        val repeated = ids.isNotEmpty() && ids == lastIds
        if (ids.isNotEmpty()) lastIds = ids
        page += 1
        if (!paged) return ShelfTake(fresh, end = true, again = false)
        val overlapped = fresh.isEmpty() && ids.isNotEmpty() && !repeated
        val again = overlapped && hasMore
        val end = !again && (batch.isEmpty() || repeated || !hasMore)
        return ShelfTake(fresh, end, again)
    }
}

package com.example.movie.data.mapper

import com.example.movie.data.remote.dto.MainResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun buildsCoverflowShelvesInReferenceOrder() {
        val payload = """
            {
              "main": [
                {"title": "В тренде", "playlist_url": "https://api.vokino.pro/v2/list?sort=popular"},
                {"title": "Продолжить просмотр", "playlist_url": "https://api.vokino.pro/v2/continue-watching/list"},
                {"title": "Обновления", "playlist_url": "https://api.vokino.pro/v2/list?sort=updatings"},
                {"title": "Топ 100", "playlist_url": "https://api.vokino.pro/v2/list?sort=watching"},
                {"title": "Фильмы", "playlist_url": "https://api.vokino.pro/v2/list?sort=popular&type=movie"},
                {"title": "Сериалы", "playlist_url": "https://api.vokino.pro/v2/list?sort=popular&type=serial"}
              ],
              "channels": [
                {"title": "ТВ Шоу", "playlist_url": "https://api.vokino.pro/v2/category?type=tvshow"},
                {"title": "Мультфильмы", "playlist_url": "https://api.vokino.pro/v2/category?type=multfilm"}
              ]
            }
        """.trimIndent()

        val shelves = json.decodeFromString<MainResponseDto>(payload).toShelves()

        assertEquals(
            listOf(
                "В тренде",
                "Сейчас смотрят",
                "4K",
                "Dolby Vision",
                "Обновления",
                "Фильмы",
                "Сериалы",
                "Мультфильмы",
            ),
            shelves.map { it.title },
        )
        assertEquals("popular:", shelves[0].id)
        assertEquals("watching", shelves[1].query.sort)
        assertEquals(null, shelves[1].query.type)
        assertEquals(true, shelves[2].query.uhd)
        assertEquals("new", shelves[2].query.sort)
        assertEquals("new::uhd", shelves[2].id)
        assertEquals(true, shelves[3].query.dolbyVision)
        assertEquals("new::dolby", shelves[3].id)
        assertEquals("multfilm", shelves[7].query.type)
    }
}

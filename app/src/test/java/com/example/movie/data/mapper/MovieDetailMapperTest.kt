package com.example.movie.data.mapper

import com.example.movie.data.remote.dto.ViewResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDetailMapperTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun mapsViewPayload_andExpandsBackdropIds() {
        val payload = """
            {
              "details": {
                "id": "6ac812adce22537d273de62b",
                "name": "Битва моторов",
                "released": 2026,
                "runtime": 144,
                "about": "1908 год.",
                "genre": "приключения, драма",
                "poster": "https://example.com/p.jpg",
                "logo_poster": "https://example.com/l.png",
                "bg_poster": {
                  "backdrop": "https://example.com/a.jpg",
                  "ids": ["one", "two"],
                  "pattern": "https://example.com/{id}.jpg"
                },
                "rating_kp": "8.4",
                "rating_imdb": "0.0",
                "age": 12
              }
            }
        """.trimIndent()

        val detail = json.decodeFromString<ViewResponseDto>(payload).details.toDetail()

        assertEquals("6ac812adce22537d273de62b", detail.id)
        assertEquals(144, detail.runtimeMinutes)
        assertEquals(listOf("приключения", "драма"), detail.genres)
        assertEquals(8.4f, detail.ratingKp)
        assertEquals(null, detail.ratingImdb)
        assertEquals(
            listOf(
                "https://example.com/a.jpg",
                "https://example.com/one.jpg",
                "https://example.com/two.jpg",
            ),
            detail.backdrops,
        )
    }

    @Test
    fun upgradesBackdropSizeToOriginal() {
        val payload = """
            {
              "details": {
                "id": "1",
                "name": "Одиссея",
                "bg_poster": {
                  "backdrop": "https://proxy.vokino.pro/image/t/p/w1280/hero.jpg",
                  "ids": ["frame"],
                  "pattern": "https://proxy.vokino.pro/image/t/p/w1280/{id}.jpg"
                }
              }
            }
        """.trimIndent()

        val detail = json.decodeFromString<ViewResponseDto>(payload).details.toDetail()

        assertEquals(
            listOf(
                "https://proxy.vokino.pro/image/t/p/original/hero.jpg",
                "https://proxy.vokino.pro/image/t/p/original/frame.jpg",
            ),
            detail.backdrops,
        )
    }

    @Test
    fun mapsCastAndRelated_andIgnoresEmptyCounters() {
        val payload = """
            {
              "details": {"id": "1", "name": "Паук", "about": "текст"},
              "casts": [
                {"id": "c1", "title": "Кит Дэвид", "poster": "https://example.com/c.jpg"},
                {"id": "c2", "title": "  "}
              ],
              "similars": 0,
              "sequelsAndPrequels": [
                {"details": {"id": "s1", "name": "Возвращение домой", "poster": "https://example.com/s.jpg", "type": "movie"}}
              ]
            }
        """.trimIndent()

        val detail = json.decodeFromString<ViewResponseDto>(payload).toMovieDetail()

        assertEquals(listOf("Кит Дэвид"), detail.cast.map { it.name })
        assertEquals(emptyList<String>(), detail.similar.map { it.title })
        assertEquals(listOf("Возвращение домой"), detail.sequels.map { it.title })
    }
}

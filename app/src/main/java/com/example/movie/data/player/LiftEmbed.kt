package com.example.movie.data.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal enum class WatchKind { Hls, Dash }

internal data class WatchSubtitle(val url: String, val name: String)

internal data class WatchStream(
    val url: String,
    val kind: WatchKind,
    val subtitles: List<WatchSubtitle>,
)

internal data class WatchEpisode(
    val season: Int,
    val episode: Int,
    val stream: WatchStream,
)

internal data class WatchPlayback(
    val stream: WatchStream,
    val episodes: List<WatchEpisode> = emptyList(),
)

internal const val LIFT_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

private val hlsUrl = Regex("""["']?hls["']?\s*:\s*"(https?:[^"]+)"""")
private val dashUrl = Regex("""["']?dash["']?\s*:\s*"(https?:[^"]+)"""")
private val ccBlock = Regex("""["']?cc["']?\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
private val ccItem = Regex("""\{\s*"url"\s*:\s*"(https?:[^"]+)"\s*,\s*"name"\s*:\s*"([^"]*)"""")
private val seasonsKey = Regex("""["']?seasons["']?\s*:\s*\[""")
private val currentEpisode = Regex(
    """["']?current["']?\s*:\s*\{[^}]*?["']?season["']?\s*:\s*"?(\d+)"?[^}]*?["']?episode["']?\s*:\s*"?(\d+)""",
)

private val json = Json { ignoreUnknownKeys = true }

private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

internal fun parseLiftEmbed(
    html: String,
    season: Int? = null,
    episode: Int? = null,
): WatchPlayback? {
    val episodes = parseEpisodes(html)
    if (episodes.isNotEmpty()) {
        val current = currentEpisode.find(html)
        val wantedSeason = season ?: current?.groupValues?.get(1)?.toIntOrNull()
        val wantedEpisode = episode ?: current?.groupValues?.get(2)?.toIntOrNull()
        val picked = episodes.firstOrNull { it.season == wantedSeason && it.episode == wantedEpisode }
            ?: episodes.firstOrNull { it.season == wantedSeason }
            ?: episodes.first()
        return WatchPlayback(picked.stream, episodes)
    }
    val single = singleStream(html) ?: return null
    return WatchPlayback(single)
}

internal fun fetchLiftStream(pageUrl: String): WatchPlayback {
    val page = pageUrl.toHttpUrlOrNull()
    val request = Request.Builder()
        .url(pageUrl)
        .header("User-Agent", LIFT_USER_AGENT)
        .build()
    client.newCall(request).execute().use { response ->
        val html = response.body?.string().orEmpty()
        if (!response.isSuccessful) error("embed ${response.code}")
        return parseLiftEmbed(
            html,
            season = page?.queryParameter("season")?.toIntOrNull(),
            episode = page?.queryParameter("episode")?.toIntOrNull(),
        ) ?: error("no direct stream")
    }
}

private fun parseEpisodes(html: String): List<WatchEpisode> {
    val key = seasonsKey.find(html) ?: return emptyList()
    val start = html.indexOf('[', key.range.first)
    val raw = extractJsonArray(html, start) ?: return emptyList()
    val seasons = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
    return seasons.mapNotNull { it as? JsonObject }.flatMap { season ->
        val number = season.int("season") ?: return@flatMap emptyList()
        val episodes = season["episodes"] as? JsonArray ?: return@flatMap emptyList()
        episodes.mapNotNull { item ->
            val episode = item as? JsonObject ?: return@mapNotNull null
            val index = episode.int("episode") ?: return@mapNotNull null
            val stream = episode.stream() ?: return@mapNotNull null
            WatchEpisode(number, index, stream)
        }
    }.sortedWith(compareBy({ it.season }, { it.episode }))
}

private fun JsonObject.stream(): WatchStream? {
    val hls = string("hls")
    val dash = string("dash") ?: string("dasha")
    val url = hls ?: dash ?: return null
    val subs = (this["cc"] as? JsonArray).orEmpty().mapNotNull { item ->
        val row = item as? JsonObject ?: return@mapNotNull null
        val subtitle = row.string("url") ?: return@mapNotNull null
        WatchSubtitle(subtitle, row.string("name").orEmpty())
    }
    return WatchStream(url, if (hls != null) WatchKind.Hls else WatchKind.Dash, subs)
}

private fun singleStream(html: String): WatchStream? {
    val hls = hlsUrl.find(html)?.groupValues?.get(1)?.cleanUrl()
    val dash = dashUrl.find(html)?.groupValues?.get(1)?.cleanUrl()
    val url = hls ?: dash ?: return null
    val subs = ccBlock.find(html)?.groupValues?.get(1).orEmpty()
        .let { block ->
            ccItem.findAll(block).map { match ->
                WatchSubtitle(match.groupValues[1].cleanUrl(), match.groupValues[2])
            }.toList()
        }
    return WatchStream(url, if (hls != null) WatchKind.Hls else WatchKind.Dash, subs)
}

private fun extractJsonArray(html: String, start: Int): String? {
    if (start < 0 || html.getOrNull(start) != '[') return null
    var depth = 0
    var inString = false
    var escaped = false
    for (index in start until html.length) {
        val char = html[index]
        if (inString) {
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inString = false
            }
            continue
        }
        when (char) {
            '"' -> inString = true
            '[' -> depth++
            ']' -> {
                depth--
                if (depth == 0) return html.substring(start, index + 1)
            }
        }
    }
    return null
}

private fun JsonObject.string(key: String): String? {
    val value = this[key] as? JsonPrimitive ?: return null
    return value.contentOrNull?.cleanUrl()?.takeIf { it.isNotBlank() }
}

private fun JsonObject.int(key: String): Int? {
    val value = this[key] as? JsonPrimitive ?: return null
    return value.intOrNull ?: value.contentOrNull?.toIntOrNull()
}

private fun String.cleanUrl(): String = replace("\\/", "/")

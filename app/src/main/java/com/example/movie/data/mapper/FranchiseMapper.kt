package com.example.movie.data.mapper

import com.example.movie.data.remote.dto.FranchiseDto
import com.example.movie.data.remote.dto.TimelineDto
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.FranchiseEpisode
import com.example.movie.domain.model.FranchiseSeason
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

internal fun TimelineDto.kpId(): Int? = kpId.asInt()

internal fun FranchiseDto.toFranchise(): Franchise? {
    if (kinopoiskId.asInt() == null && slogan.isNullOrBlank() && seasons.asObjects().isEmpty()) return null
    val seasonList = seasons.asObjects().mapNotNull { season ->
        val number = season["season"].asInt() ?: return@mapNotNull null
        FranchiseSeason(
            number = number,
            posterUrl = season.string("poster"),
            episodes = season["episodes"].asObjects().mapNotNull { episode ->
                val index = episode["episode"].asInt() ?: return@mapNotNull null
                FranchiseEpisode(
                    number = index,
                    name = episode.string("name"),
                    release = episode.string("release_ru") ?: episode.string("availability"),
                )
            }.sortedBy { it.number },
        )
    }.sortedBy { it.number }
    return Franchise(
        directors = director.names(),
        screenwriters = screenwriter.names(),
        producers = producer.names(),
        operators = operator.names(),
        editors = editor.names(),
        designers = design.names(),
        genres = genre.names(),
        countries = country.names(),
        premiere = premier.clean(),
        premiereRu = premierRus.clean(),
        slogan = slogan.clean()?.trim('«', '»', '"', ' '),
        mpaa = rateMpaa.clean(),
        budget = budget.clean(),
        feesWorld = feesWorld.clean(),
        feesRus = feesRus.clean(),
        dubActors = actorsDubl.names(),
        voiceActing = voiceActing.names(),
        serialStatus = serialStatus.clean(),
        series = type.equals("serial", ignoreCase = true) || seasonList.isNotEmpty(),
        photos = shootingPhotos.names().filter { it.startsWith("http") },
        facts = (trivia ?: facts).names().map { it.stripTags() }.filter { it.isNotBlank() },
        seasons = seasonList,
    )
}

private fun JsonElement?.names(): List<String> = when (this) {
    null, is JsonNull -> emptyList()
    is JsonPrimitive -> listOfNotNull(contentOrNull.clean())
    is JsonArray -> flatMap { element ->
        when (element) {
            is JsonPrimitive -> listOfNotNull(element.contentOrNull.clean())
            is JsonObject -> listOfNotNull(
                element.string("name") ?: element.string("title") ?: element.string("value"),
            )
            else -> emptyList()
        }
    }
    is JsonObject -> values.flatMap { it.names() }
    else -> emptyList()
}

private fun JsonElement?.asObjects(): List<JsonObject> =
    (this as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

private fun JsonObject.string(key: String): String? = this[key].asText()

private fun JsonElement?.asText(): String? = when (this) {
    null, is JsonNull -> null
    is JsonPrimitive -> contentOrNull.clean()
    else -> null
}

private fun JsonElement?.asInt(): Int? = when (this) {
    null, is JsonNull -> null
    is JsonPrimitive -> intOrNull ?: contentOrNull?.trim()?.toIntOrNull()
    else -> null
}

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() && it != "null" && it != "—" }

private fun String.stripTags(): String =
    replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .trim()

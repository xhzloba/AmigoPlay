package com.example.movie.data.player

internal fun videoTrackLabel(height: Int, bitrate: Int): String {
    if (height <= 0) return "Видео"
    if (height >= 2000) return "4K"
    return "${height}p"
}

private val audioCode = Regex("""^[A-Za-z]{2,3}(\d+)$""")

internal fun audioCodeIndex(label: String?): Int? =
    label?.let { audioCode.find(it) }?.groupValues?.get(1)?.toIntOrNull()

internal fun audioTrackLabel(label: String?, language: String?, index: Int, names: List<String>): String {
    val code = audioCodeIndex(label) ?: audioCodeIndex(language)
    if (code != null) names.getOrNull(code)?.let { return it }
    return namedTrackLabel(label, language, index)
}

internal fun namedTrackLabel(label: String?, language: String?, index: Int): String {
    label?.takeIf { it.isNotBlank() }?.let { return it }
    return when (language?.lowercase()) {
        "ru" -> "Русский"
        "en" -> "English"
        "uk" -> "Українська"
        null, "", "und" -> "Дорожка ${index + 1}"
        else -> language
    }
}

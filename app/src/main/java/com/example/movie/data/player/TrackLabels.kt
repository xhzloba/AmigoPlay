package com.example.movie.data.player

internal fun videoTrackLabel(height: Int, bitrate: Int): String {
    val picture = if (height > 0) "${height}p" else "Видео"
    if (bitrate <= 0) return picture
    return "$picture · ${bitrate / 1000} кбит/с"
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

package com.example.movie.domain.model

data class DownloadProgress(
    val read: Long = 0,
    val total: Long = 0,
    val bytesPerSecond: Long = 0,
) {
    val fraction: Float
        get() = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
}

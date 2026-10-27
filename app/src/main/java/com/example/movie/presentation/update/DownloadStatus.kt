package com.example.movie.presentation.update

import com.example.movie.domain.model.DownloadProgress
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun dashedNotes(notes: String): String =
    notes.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n") { line ->
            if (line.startsWith("—") || line.startsWith("–") || line.startsWith("-")) line
            else "— $line"
        }

internal fun downloadStatus(progress: DownloadProgress): String {
    val parts = mutableListOf<String>()
    if (progress.total > 0) {
        val percent = (progress.fraction * 100).roundToInt().coerceIn(0, 100)
        parts += "$percent%"
        parts += "${formatBytes(progress.read)} из ${formatBytes(progress.total)}"
    } else {
        parts += formatBytes(progress.read)
    }
    if (progress.bytesPerSecond > 0 || progress.read > 0) {
        parts += "${formatBytes(progress.bytesPerSecond)}/с"
    }
    return parts.joinToString(" — ")
}

private fun formatBytes(bytes: Long): String {
    val value = bytes.coerceAtLeast(0).toDouble()
    val mb = value / (1024.0 * 1024.0)
    if (mb >= 10) return "${mb.roundToInt()} МБ"
    if (mb >= 0.1) return "${oneDecimal(mb)} МБ"
    val kb = value / 1024.0
    if (kb >= 10) return "${kb.roundToInt()} КБ"
    return "${oneDecimal(kb)} КБ"
}

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10},${abs(tenths % 10)}"
}

package com.example.movie.presentation.update

import com.example.movie.domain.model.DownloadProgress
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class ReleaseNotes(
    val added: List<String>,
    val fixed: List<String>,
)

internal fun releaseNotes(body: String): ReleaseNotes {
    val added = mutableListOf<String>()
    val fixed = mutableListOf<String>()
    var section = NoteSection.Added
    body.lineSequence().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty()) return@forEach
        when (line.sectionHeader()) {
            NoteSection.Added -> section = NoteSection.Added
            NoteSection.Fixed -> section = NoteSection.Fixed
            null -> {
                val item = line.itemText()
                if (item.isEmpty()) return@forEach
                if (section == NoteSection.Fixed) fixed += item else added += item
            }
        }
    }
    return ReleaseNotes(added, fixed)
}

private enum class NoteSection { Added, Fixed }

private fun String.sectionHeader(): NoteSection? {
    val name = trim().trim('#', '*', '_', ' ').removeSuffix(":").lowercase()
    return when (name) {
        "добавлено", "added" -> NoteSection.Added
        "исправлено", "fixed" -> NoteSection.Fixed
        else -> null
    }
}

private fun String.itemText(): String =
    trim().trimStart('-', '—', '–', '*', '•').trim()

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

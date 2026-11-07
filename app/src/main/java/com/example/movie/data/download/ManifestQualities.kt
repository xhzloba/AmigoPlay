package com.example.movie.data.download

import com.example.movie.data.player.audioCodeIndex

private val hlsResolution = Regex("""RESOLUTION=(\d+)x(\d+)""")
private val dashHeight = Regex("""height\s*=\s*["'](\d+)["']""")
private val attribute = Regex("""(?:^|[,:])\s*([A-Z0-9-]+)=(?:"([^"]*)"|([^,]*))""", RegexOption.IGNORE_CASE)

internal data class HlsStreamKey(val group: Int, val index: Int)

internal fun videoHeights(manifest: String): List<Int> {
    val heights = LinkedHashSet<Int>()
    hlsResolution.findAll(manifest).forEach { match ->
        match.groupValues[2].toIntOrNull()?.takeIf { it > 0 }?.let(heights::add)
    }
    dashHeight.findAll(manifest).forEach { match ->
        match.groupValues[1].toIntOrNull()?.takeIf { it > 0 }?.let(heights::add)
    }
    return heights.sortedDescending()
}

internal fun hlsStreamKeys(manifest: String, maxHeight: Int, audioIndex: Int): List<HlsStreamKey> {
    val audios = mutableListOf<HlsAudio>()
    val variants = mutableListOf<HlsVariant>()
    var pending: HlsVariant? = null
    manifest.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXT-X-MEDIA:", ignoreCase = true) -> {
                val type = line.attr("TYPE")
                if (type.equals("AUDIO", ignoreCase = true)) {
                    audios += HlsAudio(group = line.attr("GROUP-ID").orEmpty(), name = line.attr("NAME").orEmpty())
                }
            }
            line.startsWith("#EXT-X-STREAM-INF:", ignoreCase = true) -> {
                val height = hlsResolution.find(line)?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                pending = HlsVariant(height = height, audioGroup = line.attr("AUDIO"))
            }
            pending != null && line.isNotEmpty() && !line.startsWith("#") -> {
                variants += pending
                pending = null
            }
        }
    }
    val indexed = variants.withIndex().toList()
    val usable = indexed.filter { (_, variant) ->
        variant.audioGroup?.contains("failover", ignoreCase = true) != true
    }
    val pool = usable.ifEmpty { indexed }
    val chosen = when {
        pool.isEmpty() -> return emptyList()
        maxHeight <= 0 -> pool.maxBy { it.value.height }
        else -> pool.filter { it.value.height in 1..maxHeight }.maxByOrNull { it.value.height } ?: pool.minBy { it.value.height }
    }
    val keys = mutableListOf(HlsStreamKey(GROUP_VARIANT, chosen.index))
    val group = chosen.value.audioGroup
    if (group != null) {
        val inGroup = audios.withIndex().filter { it.value.group == group }
        val audio = if (audioIndex >= 0) {
            inGroup.firstOrNull { audioCodeIndex(it.value.name) == audioIndex } ?: inGroup.getOrNull(audioIndex)
        } else {
            inGroup.firstOrNull()
        }
        if (audio != null) keys += HlsStreamKey(GROUP_AUDIO, audio.index)
    }
    return keys
}

private data class HlsAudio(val group: String, val name: String)

private data class HlsVariant(val height: Int, val audioGroup: String?)

private fun String.attr(name: String): String? =
    attribute.findAll(this).firstOrNull { it.groupValues[1].equals(name, ignoreCase = true) }?.let { match ->
        match.groupValues[2].ifEmpty { match.groupValues[3] }.trim().ifEmpty { null }
    }

private const val GROUP_VARIANT = 0
private const val GROUP_AUDIO = 1

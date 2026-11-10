package com.example.movie.data.cast

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val uriAttr = Regex("""URI="([^"]*)"""")
private val baseTag = Regex("""<BaseURL[^>]*>(.*?)</BaseURL>""", RegexOption.DOT_MATCHES_ALL)
private val mpdOpen = Regex("""<MPD\b[^>]*>""")
private val dashRef = Regex("""\b(media|initialization|sourceURL)="([^"]*)"""")

internal fun rewriteHls(text: String, playlistUrl: String, localOf: (String) -> String): String =
    text.lineSequence().joinToString("\n") { raw ->
        val line = raw.trimEnd('\r')
        when {
            line.isBlank() -> line
            line.startsWith("#") -> uriAttr.replace(line) { match ->
                val value = match.groupValues[1]
                if (value.isBlank() || value.startsWith("data:", ignoreCase = true)) match.value
                else "URI=\"${localOf(resolveUrl(playlistUrl, value))}\""
            }
            else -> localOf(resolveUrl(playlistUrl, line.trim()))
        }
    }

internal fun rewriteDash(
    xml: String,
    mpdUrl: String,
    localBase: String,
    localOf: (String) -> String,
): Pair<String, String> {
    val remoteBase = remoteBaseOf(xml, mpdUrl)
    val local = if (localBase.endsWith("/")) localBase else "$localBase/"
    val escapedLocal = xmlEscape(local)
    val withBase = if (baseTag.containsMatchIn(xml)) {
        baseTag.replace(xml, "<BaseURL>$escapedLocal</BaseURL>")
    } else {
        mpdOpen.find(xml)?.let { match ->
            xml.replaceRange(match.range, match.value + "<BaseURL>$escapedLocal</BaseURL>")
        } ?: xml
    }
    val rewritten = dashRef.replace(withBase) { match ->
        val name = match.groupValues[1]
        val value = xmlUnescape(match.groupValues[2])
        if (value.isBlank()) return@replace match.value
        val localUrl = if ('$' in value) mapTemplate(value, remoteBase, local) else localOf(resolveUrl(remoteBase, value))
        "$name=\"${xmlEscape(localUrl)}\""
    }
    return rewritten to remoteBase
}

internal fun remoteBaseOf(xml: String, mpdUrl: String): String {
    val dir = directoryOf(mpdUrl)
    val raw = baseTag.find(xml)?.groupValues?.get(1)?.trim()?.let(::xmlUnescape).orEmpty()
    if (raw.isEmpty()) return dir
    val resolved = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else resolveUrl(dir, raw)
    return if (resolved.endsWith("/")) resolved else "$resolved/"
}

internal fun directoryOf(url: String): String {
    val http = url.toHttpUrlOrNull() ?: return url
    val path = http.encodedPath.substringBeforeLast('/')
    val dir = if (path.isEmpty()) "/" else "$path/"
    return http.newBuilder().encodedPath(dir).query(null).fragment(null).build().toString()
}

internal fun resolveUrl(base: String, ref: String): String {
    val trimmed = ref.trim()
    if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) return trimmed
    return base.toHttpUrlOrNull()?.resolve(trimmed)?.toString() ?: trimmed
}

internal fun safeRelative(rel: String): Boolean {
    if (rel.isBlank() || rel.startsWith("/") || rel.startsWith("\\") || "://" in rel) return false
    return rel.split('/', '\\').none { it == ".." }
}

internal fun xmlEscape(value: String): String = buildString(value.length) {
    value.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(char)
        }
    }
}

private val variantHeight = Regex("""RESOLUTION=\d+x(\d+)""")
private val mediaAudio = Regex("""TYPE=AUDIO""", RegexOption.IGNORE_CASE)
private val mediaName = Regex("""NAME="([^"]*)"""")
private val dashHeightAttr = Regex("""\bheight\s*=\s*["'](\d+)["']""", RegexOption.IGNORE_CASE)

internal fun narrowHls(text: String, height: Int?, audioName: String?, audioIndex: Int): String {
    val lines = text.split('\n')
    val head = ArrayList<String>()
    val blocks = ArrayList<Pair<Int, List<String>>>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trimEnd('\r')
        val trimmed = line.trim()
        if (trimmed.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
            val block = ArrayList<String>()
            block += line
            var next = index + 1
            while (next < lines.size) {
                val candidate = lines[next].trimEnd('\r')
                val body = candidate.trim()
                if (body.isEmpty() || (body.startsWith("#") && !body.startsWith("#EXT-X-STREAM-INF", ignoreCase = true))) {
                    block += candidate
                    next++
                    continue
                }
                break
            }
            val uri = lines.getOrNull(next)?.trimEnd('\r')
            if (uri != null && uri.isNotBlank() && !uri.trim().startsWith("#")) {
                block += uri
                next++
            }
            val parsed = variantHeight.find(block.joinToString("\n"))?.groupValues?.get(1)?.toIntOrNull() ?: -1
            blocks += parsed to block
            index = next
            continue
        }
        if (height != null && trimmed.startsWith("#EXT-X-I-FRAME-STREAM-INF", ignoreCase = true)) {
            index++
            continue
        }
        head += line
        index++
    }
    val kept = if (height == null) blocks else blocks.filter { it.first == height }.ifEmpty { blocks }
    val audios = head.filter { line ->
        line.trim().startsWith("#EXT-X-MEDIA:", ignoreCase = true) && mediaAudio.containsMatchIn(line)
    }
    val picked = pickAudio(audios, audioName, audioIndex)
    val rewritten = if (picked == null) {
        head
    } else {
        head.map { line ->
            if (line.trim().startsWith("#EXT-X-MEDIA:", ignoreCase = true) && mediaAudio.containsMatchIn(line)) {
                flagAudio(line, line == picked)
            } else {
                line
            }
        }
    }
    return buildString {
        rewritten.forEach { append(it).append('\n') }
        kept.forEach { (_, block) -> block.forEach { append(it).append('\n') } }
    }.trimEnd('\n')
}

internal fun narrowDash(xml: String, height: Int?): String {
    if (height == null || !xml.contains("<Representation", ignoreCase = true)) return xml
    val out = StringBuilder()
    var index = 0
    var kept = false
    var dropped = false
    while (index < xml.length) {
        val start = xml.indexOf("<Representation", index, ignoreCase = true)
        if (start < 0) {
            out.append(xml, index, xml.length)
            break
        }
        out.append(xml, index, start)
        val tagEnd = xml.indexOf('>', start)
        if (tagEnd < 0) {
            out.append(xml, start, xml.length)
            break
        }
        val open = xml.substring(start, tagEnd + 1)
        val selfClosed = open.trimEnd().endsWith("/>")
        val end = if (selfClosed) {
            tagEnd + 1
        } else {
            val close = xml.indexOf("</Representation>", tagEnd, ignoreCase = true)
            if (close < 0) xml.length else close + "</Representation>".length
        }
        val parsed = dashHeightAttr.find(open)?.groupValues?.get(1)?.toIntOrNull()
        if (parsed == null || parsed == height) {
            if (parsed == height) kept = true
            out.append(xml, start, end)
        } else {
            dropped = true
        }
        index = end
    }
    return if (kept || !dropped) out.toString() else xml
}

private fun pickAudio(lines: List<String>, name: String?, index: Int): String? {
    if (lines.isEmpty()) return null
    val wanted = name?.trim().orEmpty()
    if (wanted.isNotEmpty()) {
        lines.firstOrNull { mediaName.find(it)?.groupValues?.get(1)?.equals(wanted, ignoreCase = true) == true }
            ?.let { return it }
    }
    return lines.getOrElse(index.coerceAtLeast(0)) { lines.first() }
}

private fun flagAudio(line: String, chosen: Boolean): String {
    val value = if (chosen) "YES" else "NO"
    return listOf("DEFAULT", "AUTOSELECT").fold(line) { current, key ->
        val pattern = Regex("""$key=(YES|NO)""", RegexOption.IGNORE_CASE)
        if (pattern.containsMatchIn(current)) pattern.replace(current, "$key=$value")
        else "$current,$key=$value"
    }
}

internal fun xmlUnescape(value: String): String = value
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&amp;", "&")

private fun mapTemplate(value: String, remoteBase: String, localBase: String): String {
    if (value.startsWith("http://", true) || value.startsWith("https://", true)) {
        if (value.startsWith(remoteBase)) return localBase + value.removePrefix(remoteBase)
        return localBase + value.substringAfter("://").substringAfter('/')
    }
    return localBase + value.trimStart('/')
}

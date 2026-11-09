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

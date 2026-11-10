package com.example.movie.data.cast

import android.content.Context
import com.example.movie.data.player.LIFT_USER_AGENT
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class CastProxy(
    context: Context,
    private val host: String,
) {
    private val app = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val byRemote = ConcurrentHashMap<String, String>()
    private val byToken = ConcurrentHashMap<String, String>()
    private val dashBases = ConcurrentHashMap<String, String>()
    private val assets = ConcurrentHashMap<String, ByteArray>()
    private val workers = Executors.newFixedThreadPool(6) { runnable ->
        Thread(runnable, "cast-worker").apply {
            isDaemon = true
            setUncaughtExceptionHandler { _, _ -> }
        }
    }
    private val closed = AtomicBoolean(false)
    private var server: ServerSocket? = null
    private var acceptThread: Thread? = null

    @Volatile var referer: String? = null
    @Volatile var title: String = ""
    @Volatile var playlistUrl: String = ""
    @Volatile var kind: String = ""
    @Volatile var qualityHeight: Int? = null
    @Volatile var audioIndex: Int = 0
    @Volatile var audioName: String? = null

    val origin: String
        get() {
            val port = server?.localPort ?: 0
            return "http://$host:$port"
        }

    fun start() {
        if (server != null) return
        val socket = bind()
        server = socket
        val thread = Thread({ accept(socket) }, "cast-accept")
        acceptThread = thread
        thread.start()
    }

    fun stop() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { server?.close() }
        workers.shutdownNow()
        runCatching { acceptThread?.join(500) }
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    fun localFor(remote: String): String {
        byRemote[remote]?.let { return it }
        val token = UUID.randomUUID().toString().replace("-", "").take(16)
        val url = "$origin/p/$token.${extensionOf(remote)}"
        val raced = byRemote.putIfAbsent(remote, url)
        if (raced != null) return raced
        byToken[token] = remote
        return url
    }

    private fun accept(socket: ServerSocket) {
        while (!closed.get()) {
            val clientSocket = try {
                socket.accept()
            } catch (_: Exception) {
                break
            }
            if (closed.get()) {
                runCatching { clientSocket.close() }
                break
            }
            try {
                workers.execute {
                    try {
                        serve(clientSocket)
                    } catch (_: Exception) {
                        runCatching {
                            writeBytes(clientSocket.getOutputStream(), 502, "text/plain", "upstream".toByteArray())
                        }
                    } finally {
                        runCatching { clientSocket.close() }
                    }
                }
            } catch (_: Exception) {
                runCatching { clientSocket.close() }
            }
        }
    }

    private fun serve(socket: Socket) {
        socket.tcpNoDelay = true
        socket.soTimeout = 20_000
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val head = readHead(input)
        if (head.isEmpty()) return
        val request = head[0].split(' ')
        if (request.size < 2) return
        val method = request[0].uppercase()
        val target = request[1]
        val headers = head.drop(1).associate { line ->
            val name = line.substringBefore(':').trim().lowercase()
            val value = line.substringAfter(':', "").trim()
            name to value
        }
        if (method == "OPTIONS") {
            writeHead(output, 204, "No Content", "text/plain", 0, null)
            return
        }
        if (method != "GET" && method != "HEAD") {
            writeBytes(output, 405, "text/plain", "method".toByteArray())
            return
        }
        socket.soTimeout = 0
        val path = target.substringBefore('?')
        val query = target.substringAfter('?', "")
        when {
            path == "/" || path == "/index.html" -> writeAsset(output, method, "index.html", "text/html; charset=utf-8")
            path == "/hls.min.js" -> writeAsset(output, method, "hls.min.js", "text/javascript")
            path == "/dash.all.min.js" -> writeAsset(output, method, "dash.all.min.js", "text/javascript")
            path == "/now.json" -> {
                val json = "{" +
                    "\"title\":${jsonString(title)}," +
                    "\"url\":${jsonString(playlistUrl)}," +
                    "\"kind\":${jsonString(kind)}" +
                    "}"
                writeBytes(output, 200, "application/json; charset=utf-8", json.toByteArray(), method == "HEAD")
            }
            path.startsWith("/p/") -> proxyToken(output, method, path.removePrefix("/p/"), headers["range"])
            path.startsWith("/b/") -> proxyBase(output, method, path.removePrefix("/b/"), query, headers["range"])
            else -> writeBytes(output, 404, "text/plain", "missing".toByteArray())
        }
    }

    private fun proxyToken(output: OutputStream, method: String, name: String, range: String?) {
        val token = name.substringBefore('.')
        val remote = byToken[token]
        if (remote == null) {
            writeBytes(output, 404, "text/plain", "missing".toByteArray())
            return
        }
        val playlistRequest = playlistKind(remote, "") != null
        open(remote, if (playlistRequest) null else range).use { response ->
            val type = response.header("Content-Type").orEmpty()
            val playlist = playlistKind(remote, type)
            if (playlist != null && response.isSuccessful) {
                val text = response.body?.string().orEmpty()
                val bytes = if (playlist == "dash") {
                    val (xml, base) = rewriteDash(
                        narrowDash(text, qualityHeight),
                        remote,
                        "$origin/b/$token/",
                        ::localFor,
                    )
                    dashBases[token] = base
                    xml.toByteArray()
                } else {
                    rewriteHls(narrowHls(text, qualityHeight, audioName, audioIndex), remote, ::localFor).toByteArray()
                }
                val mime = if (playlist == "dash") "application/dash+xml" else "application/vnd.apple.mpegurl"
                writeBytes(output, 200, mime, bytes, method == "HEAD")
            } else {
                pipe(output, method, response)
            }
        }
    }

    private fun proxyBase(output: OutputStream, method: String, rest: String, query: String, range: String?) {
        val token = rest.substringBefore('/')
        val rel = rest.substringAfter('/', "")
        val base = dashBases[token]
        if (base == null || !safeRelative(rel)) {
            writeBytes(output, 404, "text/plain", "missing".toByteArray())
            return
        }
        val resolved = base.toHttpUrlOrNull()?.resolve(rel)
        val remoteBase = base.toHttpUrlOrNull()
        if (resolved == null || remoteBase == null || resolved.host != remoteBase.host) {
            writeBytes(output, 400, "text/plain", "bad".toByteArray())
            return
        }
        val remote = if (query.isBlank()) resolved.toString() else resolved.newBuilder().query(query).build().toString()
        open(remote, range).use { response -> pipe(output, method, response) }
    }

    private fun pipe(output: OutputStream, method: String, response: okhttp3.Response) {
        val body = response.body
        val length = body?.contentLength() ?: 0L
        val type = mediaType(response.request.url.toString(), response.header("Content-Type"))
        val reasonText = response.message.ifBlank { reason(response.code) }
        writeHead(output, response.code, reasonText, type, length, response.header("Content-Range"))
        if (method == "HEAD" || body == null) return
        body.byteStream().use { input ->
            if (length >= 0) input.copyTo(output) else writeChunked(input, output)
        }
        output.flush()
    }

    private fun open(url: String, range: String?): okhttp3.Response =
        try {
            openOnce(url, range)
        } catch (_: java.io.IOException) {
            openOnce(url, range)
        }

    private fun openOnce(url: String, range: String?): okhttp3.Response {
        val first = call(url, referer, range)
        if (first.code != 403 && first.code != 401) return first
        val fallback = url.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}/" }
        if (fallback.isNullOrBlank() || fallback == referer) return first
        first.close()
        return call(url, fallback, range)
    }

    private fun call(url: String, referer: String?, range: String?): okhttp3.Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", LIFT_USER_AGENT)
            .header("Accept", "*/*")
            .apply {
                if (!referer.isNullOrBlank()) header("Referer", referer)
                if (!range.isNullOrBlank()) header("Range", range)
            }
            .build()
        return client.newCall(request).execute()
    }

    private fun writeAsset(output: OutputStream, method: String, name: String, type: String) {
        val bytes = assets.getOrPut(name) {
            app.assets.open("cast/$name").use { it.readBytes() }
        }
        writeBytes(output, 200, type, bytes, method == "HEAD")
    }

    private fun readHead(input: InputStream): List<String> {
        val lines = ArrayList<String>(16)
        val buffer = ByteArrayOutputStream()
        while (lines.size < 48) {
            val byte = input.read()
            if (byte < 0) break
            if (byte == '\n'.code) {
                val line = buffer.toString(Charsets.ISO_8859_1).trimEnd('\r')
                buffer.reset()
                if (line.isEmpty()) break
                lines += line
            } else {
                buffer.write(byte)
            }
        }
        return lines
    }
}

private fun bind(): ServerSocket =
    runCatching { listen(8765) }.getOrNull() ?: listen(0)

private fun listen(port: Int): ServerSocket =
    ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(port), 24)
    }

private fun playlistKind(remote: String, contentType: String): String? {
    val path = remote.substringBefore('?').lowercase()
    val type = contentType.lowercase()
    return when {
        path.endsWith(".m3u8") || path.endsWith(".m3u") || "mpegurl" in type -> "hls"
        path.endsWith(".mpd") || "dash+xml" in type -> "dash"
        else -> null
    }
}

private fun extensionOf(remote: String): String {
    val name = remote.substringBefore('?').substringBefore('#').substringAfterLast('/')
    val ext = name.substringAfterLast('.', "")
    return if (ext.length in 1..5 && ext.all { it.isLetterOrDigit() }) ext.lowercase() else "bin"
}

private fun mediaType(remote: String, upstream: String?): String {
    val path = remote.substringBefore('?').lowercase()
    return when {
        path.endsWith(".m3u8") || path.endsWith(".m3u") -> "application/vnd.apple.mpegurl"
        path.endsWith(".mpd") -> "application/dash+xml"
        path.endsWith(".ts") -> "video/mp2t"
        path.endsWith(".mp4") || path.endsWith(".m4s") || path.endsWith(".cmfv") || path.endsWith(".cmfa") -> "video/mp4"
        path.endsWith(".vtt") -> "text/vtt"
        path.endsWith(".aac") -> "audio/aac"
        path.endsWith(".mp3") -> "audio/mpeg"
        !upstream.isNullOrBlank() && "text/html" !in upstream.lowercase() -> upstream.substringBefore(';')
        else -> "application/octet-stream"
    }
}

private fun reason(code: Int): String = when (code) {
    200 -> "OK"
    206 -> "Partial Content"
    403 -> "Forbidden"
    404 -> "Not Found"
    else -> "OK"
}

private fun writeBytes(output: OutputStream, code: Int, type: String, bytes: ByteArray, head: Boolean = false) {
    writeHead(output, code, reason(code), type, bytes.size.toLong(), null)
    if (!head) output.write(bytes)
    output.flush()
}

private fun writeHead(output: OutputStream, code: Int, reason: String, type: String, length: Long, range: String?) {
    val head = buildString {
        append("HTTP/1.1 ").append(code).append(' ').append(reason).append("\r\n")
        append("Content-Type: ").append(type).append("\r\n")
        append("Access-Control-Allow-Origin: *\r\n")
        append("Accept-Ranges: bytes\r\n")
        append("Cache-Control: no-store\r\n")
        if (!range.isNullOrBlank()) append("Content-Range: ").append(range).append("\r\n")
        if (length >= 0) append("Content-Length: ").append(length).append("\r\n")
        else append("Transfer-Encoding: chunked\r\n")
        append("Connection: close\r\n\r\n")
    }
    output.write(head.toByteArray(Charsets.ISO_8859_1))
}

private fun writeChunked(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(16 * 1024)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read == 0) continue
        output.write(read.toString(16).toByteArray(Charsets.US_ASCII))
        output.write("\r\n".toByteArray(Charsets.US_ASCII))
        output.write(buffer, 0, read)
        output.write("\r\n".toByteArray(Charsets.US_ASCII))
    }
    output.write("0\r\n\r\n".toByteArray(Charsets.US_ASCII))
}

private fun jsonString(value: String): String = buildString(value.length + 2) {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(char)
        }
    }
    append('"')
}

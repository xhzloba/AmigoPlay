package com.example.movie.data.cast

import android.content.Context
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal class TvPairing(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tv_pairing", Context.MODE_PRIVATE)

    fun samsung(host: String): String? = prefs.getString("samsung:$host", null)?.takeIf { it.isNotBlank() }

    fun saveSamsung(host: String, token: String) {
        prefs.edit().putString("samsung:$host", token).apply()
    }

    fun clearSamsung(host: String) {
        prefs.edit().remove("samsung:$host").apply()
    }

    fun lg(host: String): String? = prefs.getString("lg:$host", null)?.takeIf { it.isNotBlank() }

    fun saveLg(host: String, key: String) {
        prefs.edit().putString("lg:$host", key).apply()
    }
}

internal fun openTvBrowser(host: String, page: String, pairing: TvPairing) {
    val saved = pairing.samsung(host)
    var attempt = samsung(host, page, pairing, saved)
    if (attempt == Attempt.Refused && !saved.isNullOrBlank()) {
        attempt = samsung(host, page, pairing, null)
    }
    if (attempt == Attempt.Opened) return
    if (attempt == Attempt.Missed) {
        throw IllegalStateException("На телевизоре не подтвердили доступ. Нажми «Браузер» ещё раз.")
    }
    when (lg(host, page, pairing)) {
        Attempt.Opened -> return
        Attempt.Missed -> throw IllegalStateException("На телевизоре не подтвердили доступ. Нажми «Браузер» ещё раз.")
        Attempt.Refused -> throw IllegalStateException("Браузер телевизора не открылся. Открой адрес из шторки вручную.")
    }
}

internal fun samsungLaunch(url: String): String =
    """{"method":"ms.channel.emit","params":{"event":"ed.apps.launch","to":"host","data":{"appId":"org.tizen.browser","action_type":"DEEP_LINK","metaTag":${json(url)}}}}"""

internal fun lgLaunch(url: String): String =
    """{"id":"launch","type":"request","uri":"ssap://system.launcher/launch","payload":{"id":"com.webos.app.browser","params":{"target":${json(url)},"url":${json(url)}}}}"""

private fun samsung(host: String, page: String, pairing: TvPairing, token: String?): Attempt {
    val plain = samsungPort(host, 8001, false, token, page, pairing)
    if (plain != Attempt.Refused) return plain
    return samsungPort(host, 8002, true, token, page, pairing)
}

private fun samsungPort(
    host: String,
    port: Int,
    tls: Boolean,
    token: String?,
    page: String,
    pairing: TvPairing,
): Attempt {
    val name = Base64.encodeToString("AmigoPlay".toByteArray(), Base64.NO_WRAP)
    val query = buildString {
        append("name=").append(name)
        if (!token.isNullOrBlank()) append("&token=").append(token)
    }
    val scheme = if (tls) "wss" else "ws"
    val gate = Gate()
    val socket = connect("$scheme://$host:$port/api/v2/channels/samsung.remote.control?$query", tls, gate) { webSocket, text ->
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return@connect
        when (json.optString("event")) {
            "ms.channel.unauthorized", "ms.channel.timeOut" -> {
                pairing.clearSamsung(host)
                webSocket.close(1000, null)
                gate.finish(success = false, refused = true)
            }
            "ms.channel.connect" -> {
                json.optJSONObject("data")?.optString("token").orEmpty().takeIf { it.isNotBlank() }?.let {
                    pairing.saveSamsung(host, it)
                }
                webSocket.send(samsungLaunch(page))
                webSocket.close(1000, null)
                gate.finish(true)
            }
        }
    }
    return finish(socket, gate, 25)
}

private fun lg(host: String, page: String, pairing: TvPairing): Attempt {
    val plain = lgPort(host, 3000, false, page, pairing)
    if (plain != Attempt.Refused) return plain
    return lgPort(host, 3001, true, page, pairing)
}

private fun lgPort(host: String, port: Int, tls: Boolean, page: String, pairing: TvPairing): Attempt {
    val gate = Gate()
    val scheme = if (tls) "wss" else "ws"
    val socket = connect("$scheme://$host:$port/", tls, gate) { webSocket, text ->
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return@connect
        val type = json.optString("type")
        val id = json.optString("id")
        if (type == "registered") {
            json.optJSONObject("payload")?.optString("client-key").orEmpty().takeIf { it.isNotBlank() }?.let {
                pairing.saveLg(host, it)
            }
            webSocket.send(lgLaunch(page))
        } else if (id == "launch" && type == "response") {
            val ok = json.optJSONObject("payload")?.optBoolean("returnValue") == true
            webSocket.close(1000, null)
            gate.finish(ok)
        }
    }
    socket?.send(lgRegister(pairing.lg(host)))
    return finish(socket, gate, 25)
}

private fun finish(socket: WebSocket?, gate: Gate, seconds: Long): Attempt {
    if (socket == null) return Attempt.Refused
    val attempt = gate.await(seconds)
    runCatching { socket.cancel() }
    return attempt
}

private fun connect(
    url: String,
    tls: Boolean,
    gate: Gate,
    onText: (WebSocket, String) -> Unit,
): WebSocket? {
    val client = if (tls) insecureClient else plainClient
    val request = Request.Builder().url(url).build()
    return runCatching {
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                gate.markOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) = onText(webSocket, text)

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                gate.finish(success = false, refused = !gate.socketOpen)
            }
        })
    }.getOrNull()
}

private enum class Attempt {
    Opened,
    Refused,
    Missed,
}

private class Gate {
    private val latch = CountDownLatch(1)
    private val once = AtomicBoolean(false)

    @Volatile
    var socketOpen: Boolean = false
        private set

    fun markOpen() {
        socketOpen = true
    }

    fun finish(success: Boolean, refused: Boolean = false) {
        if (!once.compareAndSet(false, true)) return
        result = when {
            success -> Attempt.Opened
            refused || !socketOpen -> Attempt.Refused
            else -> Attempt.Missed
        }
        latch.countDown()
    }

    fun await(seconds: Long): Attempt {
        val done = latch.await(seconds, TimeUnit.SECONDS)
        if (!done) return if (socketOpen) Attempt.Missed else Attempt.Refused
        return result
    }

    @Volatile
    private var result: Attempt = Attempt.Missed
}

private fun lgRegister(clientKey: String?): String {
    val payload = JSONObject()
        .put("forcePairing", false)
        .put("pairingType", "PROMPT")
        .put("manifest", lgManifest())
    if (!clientKey.isNullOrBlank()) payload.put("client-key", clientKey)
    return JSONObject()
        .put("type", "register")
        .put("id", "reg")
        .put("payload", payload)
        .toString()
}

private fun lgManifest(): JSONObject {
    val permissions = JSONArray()
    listOf(
        "LAUNCH",
        "LAUNCH_WEBAPP",
        "APP_TO_APP",
        "CLOSE",
        "CONTROL_AUDIO",
        "CONTROL_DISPLAY",
        "CONTROL_INPUT_MEDIA_PLAYBACK",
        "CONTROL_POWER",
        "READ_APP_STATUS",
        "READ_NETWORK_STATE",
        "READ_RUNNING_APPS",
    ).forEach { permissions.put(it) }
    return JSONObject()
        .put("manifestVersion", 1)
        .put("appVersion", "1.1")
        .put("permissions", permissions)
        .put(
            "signatures",
            JSONArray().put(
                JSONObject()
                    .put("signatureVersion", 1)
                    .put("signature", LgSignature),
            ),
        )
        .put(
            "signed",
            JSONObject()
                .put("created", "20140509")
                .put("appId", "com.lge.test")
                .put("vendorId", "com.lge")
                .put("serial", "2f930e2d2cfe083771f68e4fe7bb07")
                .put(
                    "localizedAppNames",
                    JSONObject().put("", "AmigoPlay").put("ru-RU", "AmigoPlay"),
                )
                .put("localizedVendorNames", JSONObject().put("", "AmigoPlay"))
                .put("permissions", JSONArray().put("LAUNCH").put("CONTROL_INPUT_MEDIA_PLAYBACK")),
        )
}

private fun json(value: String): String = buildString(value.length + 2) {
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

private val plainClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .build()

private val insecureClient: OkHttpClient by lazy {
    val trust = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    val context = SSLContext.getInstance("TLS")
    context.init(null, arrayOf<TrustManager>(trust), SecureRandom())
    plainClient.newBuilder()
        .sslSocketFactory(context.socketFactory, trust)
        .hostnameVerifier { _, _ -> true }
        .build()
}

private const val LgSignature =
    "eyJhbGdvcml0aG0iOiJSU0EtU0hBMjU2Iiwia2V5SWQiOiJ0ZXN0LXNpZ25pbmctY2VydCIsInNpZ25hdHVyZVZlcnNpb24iOjF9." +
        "hrVRgjCwXVvE2OOSpDZ58hR+59aFNwYDyjQgKk3auukd7pcegmE2CzPCa0bJ0ZsRAcKkCTJrWo5iDzNhMBWRyaMOv5zWSrthlf7G128qvIlpMT0YNY+" +
        "n/FaOHE73uLrS/g7swl3/qH/BGFG2Hu4RlL48eb3lLKqTt2xKHdCs6Cd4RMfJPYnzgvI4BNrFUKsjkcu+WD4OO2A27Pq1n50cMchmcaXadJhGrOqH5YmHdOCj5NSHzJYrsW0HPlpuAx/" +
        "ECMeIZYDh6RMqaFM2DXzdKX9NmmyqzJ3o/0lkk/N97gfVRLW5hA29yeAwaCViZNCP8iC9aO0q9fQojoa7NQnAtw=="

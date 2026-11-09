package com.example.movie.data.cast

import android.content.Context
import android.net.wifi.WifiManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class DlnaDevice(
    val name: String,
    val controlUrl: String,
    val location: String,
)

private const val AvTransport = "urn:schemas-upnp-org:service:AVTransport:1"
private val soapType = "text/xml; charset=utf-8".toMediaType()
private val serviceBlock = Regex("<service>(.*?)</service>", RegexOption.DOT_MATCHES_ALL)
private val controlTag = Regex("<controlURL>(.*?)</controlURL>", RegexOption.DOT_MATCHES_ALL)
private val nameTag = Regex("<friendlyName>(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?</friendlyName>", RegexOption.DOT_MATCHES_ALL)
private val faultTag = Regex("<faultstring>(.*?)</faultstring>", RegexOption.DOT_MATCHES_ALL)

private val dlnaClient = OkHttpClient.Builder()
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

internal fun discoverRenderers(context: Context, host: String): List<DlnaDevice> {
    if (host.isBlank()) return emptyList()
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    val lock = wifi?.createMulticastLock("movie:cast-ssdp")?.apply {
        setReferenceCounted(false)
        acquire()
    }
    try {
        val locations = search(host)
        val found = LinkedHashMap<String, DlnaDevice>()
        locations.forEach { location ->
            val xml = fetchText(location) ?: return@forEach
            val device = parseDevice(xml, location) ?: return@forEach
            found.putIfAbsent(device.controlUrl, device)
        }
        return found.values.toList()
    } finally {
        if (lock?.isHeld == true) lock.release()
    }
}

internal fun parseDevice(xml: String, location: String): DlnaDevice? {
    val base = location.toHttpUrlOrNull() ?: return null
    val service = serviceBlock.findAll(xml).firstOrNull { block ->
        block.groupValues[1].contains("AVTransport")
    } ?: return null
    val control = controlTag.find(service.groupValues[1])?.groupValues?.get(1)?.trim().orEmpty()
    if (control.isEmpty()) return null
    val absolute = base.resolve(xmlUnescape(control))?.toString() ?: return null
    val name = nameTag.find(xml)?.groupValues?.get(1)?.trim()?.let(::xmlUnescape).orEmpty()
    return DlnaDevice(name.ifBlank { "Телевизор" }, absolute, location)
}

internal fun playOnRenderer(device: DlnaDevice, mediaUrl: String, title: String) {
    runCatching { invoke(device.controlUrl, "Stop", "") }
    try {
        setUri(device.controlUrl, mediaUrl, title, withMeta = true)
    } catch (_: Exception) {
        setUri(device.controlUrl, mediaUrl, title, withMeta = false)
    }
    Thread.sleep(300)
    invoke(device.controlUrl, "Play", "<Speed>1</Speed>")
}

internal fun stopRenderer(controlUrl: String) {
    invoke(controlUrl, "Stop", "")
}

private fun setUri(controlUrl: String, mediaUrl: String, title: String, withMeta: Boolean) {
    val meta = if (withMeta) xmlEscape(didl(title, mediaUrl)) else ""
    invoke(
        controlUrl,
        "SetAVTransportURI",
        "<CurrentURI>${xmlEscape(mediaUrl)}</CurrentURI><CurrentURIMetaData>$meta</CurrentURIMetaData>",
    )
}

private fun didl(title: String, url: String): String =
    "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
        "<item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
        "<dc:title>${xmlEscape(title)}</dc:title>" +
        "<upnp:class>object.item.videoItem</upnp:class>" +
        "<res protocolInfo=\"http-get:*:application/vnd.apple.mpegurl:*\">${xmlEscape(url)}</res>" +
        "</item></DIDL-Lite>"

private fun invoke(controlUrl: String, action: String, inner: String) {
    val body = """
        <?xml version="1.0" encoding="utf-8"?>
        <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
        <s:Body>
        <u:$action xmlns:u="$AvTransport"><InstanceID>0</InstanceID>$inner</u:$action>
        </s:Body>
        </s:Envelope>
    """.trimIndent()
    val request = Request.Builder()
        .url(controlUrl)
        .header("Content-Type", "text/xml; charset=\"utf-8\"")
        .header("SOAPAction", "\"$AvTransport#$action\"")
        .header("User-Agent", "AmigoPlay")
        .post(body.toRequestBody(soapType))
        .build()
    dlnaClient.newCall(request).execute().use { response ->
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful || "<Fault" in text) {
            val fault = faultTag.find(text)?.groupValues?.get(1)?.trim()
            error(fault?.ifBlank { null } ?: "SOAP ${response.code}")
        }
    }
}

private fun search(host: String): List<String> {
    val locations = LinkedHashSet<String>()
    DatagramSocket(null).use { socket ->
        socket.reuseAddress = true
        try {
            socket.bind(InetSocketAddress(InetAddress.getByName(host), 0))
        } catch (_: Exception) {
            socket.bind(InetSocketAddress(0))
        }
        socket.soTimeout = 400
        val group = InetAddress.getByName("239.255.255.250")
        listOf(
            "urn:schemas-upnp-org:device:MediaRenderer:1",
            "urn:schemas-upnp-org:service:AVTransport:1",
        ).forEach { target ->
            val bytes = msearch(target).toByteArray(Charsets.UTF_8)
            repeat(2) { socket.send(DatagramPacket(bytes, bytes.size, group, 1900)) }
        }
        val deadline = System.nanoTime() + 4_000_000_000L
        val buffer = ByteArray(8192)
        while (System.nanoTime() < deadline && locations.size < 24) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (_: SocketTimeoutException) {
                continue
            }
            val text = String(packet.data, 0, packet.length, Charsets.ISO_8859_1)
            text.lineSequence().forEach { line ->
                if (line.startsWith("LOCATION:", ignoreCase = true)) {
                    val value = line.substringAfter(':').trim()
                    if (value.startsWith("http", ignoreCase = true)) locations += value
                }
            }
        }
    }
    return locations.toList()
}

private fun msearch(target: String): String =
    "M-SEARCH * HTTP/1.1\r\n" +
        "HOST: 239.255.255.250:1900\r\n" +
        "MAN: \"ssdp:discover\"\r\n" +
        "MX: 2\r\n" +
        "ST: $target\r\n\r\n"

private fun fetchText(url: String): String? = runCatching {
    val request = Request.Builder().url(url).header("User-Agent", "AmigoPlay").build()
    dlnaClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        response.body?.string()
    }
}.getOrNull()

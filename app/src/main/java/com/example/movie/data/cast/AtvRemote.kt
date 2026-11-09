package com.example.movie.data.cast

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal sealed class AtvResult {
    data object Launched : AtvResult()
    data object NoLink : AtvResult()
    data object Offline : AtvResult()
    class Pin(val session: AtvSession) : AtvResult()
}

internal enum class AtvPin {
    Ok,
    Again,
    Rejected,
}

internal class AtvIdentity(
    val privateKey: PrivateKey,
    val certificate: X509Certificate,
) {
    fun context(): SSLContext {
        val store = KeyStore.getInstance(KeyStore.getDefaultType())
        store.load(null)
        store.setKeyEntry("client", privateKey, CharArray(0), arrayOf(certificate))
        val keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keys.init(store, CharArray(0))
        val context = SSLContext.getInstance("TLS")
        context.init(keys.keyManagers, arrayOf(trustAll), SecureRandom())
        return context
    }

    companion object {
        fun load(directory: File): AtvIdentity {
            val keyFile = File(directory, "atv_key.pem")
            val certFile = File(directory, "atv_cert.pem")
            if (!keyFile.exists() || !certFile.exists()) {
                val created = generate()
                keyFile.writeText(pem("PRIVATE KEY", created.privateKey.encoded))
                certFile.writeText(pem("CERTIFICATE", created.certificate.encoded))
                return created
            }
            val privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(decodePem(keyFile.readText())))
            val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(decodePem(certFile.readText()).inputStream()) as X509Certificate
            return AtvIdentity(privateKey, certificate)
        }

        fun generate(): AtvIdentity {
            val provider = BouncyCastleProvider()
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048, SecureRandom())
            val pair = generator.generateKeyPair()
            val now = System.currentTimeMillis()
            val name = X500Name("CN=AmigoPlay")
            val holder = JcaX509v3CertificateBuilder(
                name,
                BigInteger.valueOf(now),
                Date(now - 60_000),
                Date(now + 10L * 365 * 24 * 60 * 60 * 1000),
                name,
                pair.public,
            ).build(JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.private))
            val certificate = JcaX509CertificateConverter().setProvider(provider).getCertificate(holder)
            return AtvIdentity(pair.private, certificate)
        }
    }
}

internal class AtvSession(
    private val socket: SSLSocket,
    private val clientModulus: BigInteger,
    private val clientExponent: BigInteger,
    private val serverModulus: BigInteger,
    private val serverExponent: BigInteger,
) {
    private val closed = AtomicBoolean(false)

    fun finish(pin: String): AtvPin {
        val hash = pinHash(clientModulus, clientExponent, serverModulus, serverExponent, pin) ?: return AtvPin.Again
        val result = try {
            socket.soTimeout = 10_000
            writeFrame(socket.outputStream, secretMessage(hash))
            val deadline = System.nanoTime() + 10_000_000_000L
            var outcome = AtvPin.Rejected
            while (System.nanoTime() < deadline) {
                val fields = parseProto(readFrame(socket.inputStream))
                if ((fields.num(2) ?: 200L) != 200L) break
                if (fields.has(41)) {
                    outcome = AtvPin.Ok
                    break
                }
            }
            outcome
        } catch (_: Exception) {
            AtvPin.Rejected
        }
        close()
        return result
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { socket.close() }
    }
}

internal fun atvOpen(host: String, link: String, identity: AtvIdentity): AtvResult {
    try {
        launchOnCommand(host, link, identity)
        return AtvResult.Launched
    } catch (_: AtvNoLink) {
        return AtvResult.NoLink
    } catch (error: Exception) {
        if (!unpaired(error)) return AtvResult.Offline
    }
    return try {
        AtvResult.Pin(startPairing(host, identity))
    } catch (_: Exception) {
        AtvResult.Offline
    }
}

internal fun pinHash(
    clientModulus: BigInteger,
    clientExponent: BigInteger,
    serverModulus: BigInteger,
    serverExponent: BigInteger,
    pin: String,
): ByteArray? {
    val code = pin.uppercase().filter { it.isDigit() || it in 'A'..'F' }
    if (code.length != 6) return null
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(unsigned(clientModulus))
    digest.update(unsigned(clientExponent))
    digest.update(unsigned(serverModulus))
    digest.update(unsigned(serverExponent))
    digest.update(hexBytes(code.substring(2)))
    val hash = digest.digest()
    if ((hash[0].toInt() and 0xff) != code.substring(0, 2).toInt(16)) return null
    return hash
}

internal fun unsigned(value: BigInteger): ByteArray {
    val raw = value.toByteArray()
    return if (raw.size > 1 && raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
}

internal fun launchFrame(link: String): ByteArray = launchMessage(link)

private class AtvNoLink : Exception()

private val trustAll = object : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

private const val COMMAND_PORT = 6466
private const val PAIR_PORT = 6467
private const val FEATURE_PING = 1
private const val FEATURE_KEY = 2
private const val FEATURE_POWER = 32
private const val FEATURE_VOLUME = 64
private const val FEATURE_APP_LINK = 512
private const val OUR_FEATURES = FEATURE_PING or FEATURE_KEY or FEATURE_POWER or FEATURE_VOLUME or FEATURE_APP_LINK

private fun launchOnCommand(host: String, link: String, identity: AtvIdentity) {
    val socket = connect(host, COMMAND_PORT, identity, 4_000)
    try {
        val features = untilStart(socket)
        if (features and FEATURE_APP_LINK == 0) throw AtvNoLink()
        writeFrame(socket.outputStream, launchMessage(link))
        drain(socket)
    } finally {
        runCatching { socket.close() }
    }
}

private fun startPairing(host: String, identity: AtvIdentity): AtvSession {
    val socket = connect(host, PAIR_PORT, identity, 4_000)
    try {
        socket.soTimeout = 10_000
        val output = socket.outputStream
        writeFrame(output, pairingRequest())
        readUntil(socket, 11)
        writeFrame(output, pairingOptions())
        readUntil(socket, 20)
        writeFrame(output, pairingConfiguration())
        readUntil(socket, 31)
        val server = (socket.session.peerCertificates.first() as X509Certificate).publicKey as RSAPublicKey
        val client = identity.certificate.publicKey as RSAPublicKey
        return AtvSession(socket, client.modulus, client.publicExponent, server.modulus, server.publicExponent)
    } catch (error: Exception) {
        runCatching { socket.close() }
        throw error
    }
}

private fun untilStart(socket: SSLSocket): Int {
    socket.soTimeout = 8_000
    val output = socket.outputStream
    val deadline = System.nanoTime() + 8_000_000_000L
    var features = OUR_FEATURES
    while (System.nanoTime() < deadline) {
        val fields = parseProto(readFrame(socket.inputStream))
        when {
            fields.has(1) -> {
                val code = parseProto(fields.blob(1) ?: ByteArray(0)).num(1)?.toInt() ?: 0
                if (code != 0) features = OUR_FEATURES and code
                writeFrame(output, configureReply(features))
            }
            fields.has(2) -> writeFrame(output, setActiveReply(features))
            fields.has(8) -> {
                val value = parseProto(fields.blob(8) ?: ByteArray(0)).num(1)?.toInt() ?: 0
                writeFrame(output, pingReply(value))
            }
            fields.has(40) -> return features
            fields.has(3) -> throw SocketException("телевизор отклонил пульт")
        }
    }
    throw SocketTimeoutException("телевизор не ответил")
}

private fun drain(socket: SSLSocket) {
    socket.soTimeout = 800
    val output = socket.outputStream
    repeat(3) {
        try {
            val fields = parseProto(readFrame(socket.inputStream))
            if (fields.has(3)) throw SocketException("телевизор отклонил ссылку")
            if (fields.has(8)) {
                val value = parseProto(fields.blob(8) ?: ByteArray(0)).num(1)?.toInt() ?: 0
                writeFrame(output, pingReply(value))
            }
        } catch (_: SocketTimeoutException) {
            return
        }
    }
}

private fun readUntil(socket: SSLSocket, field: Int) {
    val deadline = System.nanoTime() + 10_000_000_000L
    while (System.nanoTime() < deadline) {
        val fields = parseProto(readFrame(socket.inputStream))
        if ((fields.num(2) ?: 200L) != 200L) throw SocketException("телевизор отклонил сопряжение")
        if (fields.has(field)) return
    }
    throw SocketTimeoutException("телевизор не ответил")
}

private fun connect(host: String, port: Int, identity: AtvIdentity, timeoutMs: Int): SSLSocket {
    val socket = identity.context().socketFactory.createSocket() as SSLSocket
    socket.tcpNoDelay = true
    socket.soTimeout = timeoutMs
    try {
        socket.connect(InetSocketAddress(host, port), timeoutMs)
        socket.startHandshake()
    } catch (error: Exception) {
        runCatching { socket.close() }
        throw error
    }
    return socket
}

private fun unpaired(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        if (current is SSLException) return true
        if (current is SocketTimeoutException || current is ConnectException) return false
        val text = current.message.orEmpty()
        if (text.contains("reset", true) || text.contains("handshake", true)) return true
        current = current.cause
    }
    return false
}

private fun pairingRequest(): ByteArray = proto {
    varint(1, 2)
    varint(2, 200)
    message(10) {
        text(1, "atvremote")
        text(2, "AmigoPlay")
    }
}

private fun pairingOptions(): ByteArray = proto {
    varint(1, 2)
    varint(2, 200)
    message(20) {
        message(1) {
            varint(1, 3)
            varint(2, 6)
        }
        varint(3, 1)
    }
}

private fun pairingConfiguration(): ByteArray = proto {
    varint(1, 2)
    varint(2, 200)
    message(30) {
        message(1) {
            varint(1, 3)
            varint(2, 6)
        }
        varint(2, 1)
    }
}

private fun secretMessage(hash: ByteArray): ByteArray = proto {
    varint(1, 2)
    varint(2, 200)
    message(40) { bytes(1, hash) }
}

private fun configureReply(features: Int): ByteArray = proto {
    message(1) {
        varint(1, features)
        message(2) {
            varint(3, 1)
            text(4, "1")
            text(5, "atvremote")
            text(6, "1.0.0")
        }
    }
}

private fun setActiveReply(features: Int): ByteArray = proto {
    message(2) { varint(1, features) }
}

private fun pingReply(value: Int): ByteArray = proto {
    message(9) { varint(1, value) }
}

private fun launchMessage(link: String): ByteArray = proto {
    message(90) { text(1, link) }
}

private class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun varint(field: Int, value: Int) {
        tag(field, 0)
        writeVarint(value.toLong() and 0xffffffffL)
    }

    fun text(field: Int, value: String) = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun bytes(field: Int, value: ByteArray) {
        tag(field, 2)
        writeVarint(value.size.toLong())
        out.write(value)
    }

    fun message(field: Int, fill: ProtoWriter.() -> Unit) {
        bytes(field, proto(fill))
    }

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun tag(field: Int, wire: Int) = writeVarint(((field shl 3) or wire).toLong())

    private fun writeVarint(value: Long) {
        var current = value
        while (true) {
            val bits = (current and 0x7f).toInt()
            current = current ushr 7
            if (current == 0L) {
                out.write(bits)
                return
            }
            out.write(bits or 0x80)
        }
    }
}

private fun proto(fill: ProtoWriter.() -> Unit): ByteArray = ProtoWriter().apply(fill).toByteArray()

internal class ProtoMap(private val fields: Map<Int, List<ProtoPart>>) {
    fun has(field: Int): Boolean = fields.containsKey(field)

    fun num(field: Int): Long? = (fields[field]?.lastOrNull() as? ProtoPart.Num)?.value

    fun blob(field: Int): ByteArray? = (fields[field]?.lastOrNull() as? ProtoPart.Blob)?.value
}

internal sealed class ProtoPart {
    class Num(val value: Long) : ProtoPart()
    class Blob(val value: ByteArray) : ProtoPart()
}

internal fun parseProto(data: ByteArray): ProtoMap {
    val fields = linkedMapOf<Int, MutableList<ProtoPart>>()
    var pos = 0
    while (pos < data.size) {
        val tag = readVarint(data, pos)
        pos = tag.next
        val field = (tag.value ushr 3).toInt()
        when (val wire = (tag.value and 7).toInt()) {
            0 -> {
                val value = readVarint(data, pos)
                pos = value.next
                fields.getOrPut(field) { mutableListOf() }.add(ProtoPart.Num(value.value))
            }
            2 -> {
                val length = readVarint(data, pos)
                pos = length.next
                val size = length.value.toInt()
                if (size < 0 || pos + size > data.size) throw SocketException("битый ответ телевизора")
                fields.getOrPut(field) { mutableListOf() }.add(ProtoPart.Blob(data.copyOfRange(pos, pos + size)))
                pos += size
            }
            1 -> pos += 8
            5 -> pos += 4
            else -> throw SocketException("битый ответ телевизора")
        }
        if (pos > data.size) throw SocketException("битый ответ телевизора")
    }
    return ProtoMap(fields)
}

private class Cursor(val value: Long, val next: Int)

private fun readVarint(data: ByteArray, start: Int): Cursor {
    var result = 0L
    var shift = 0
    var pos = start
    while (pos < data.size && shift <= 63) {
        val byte = data[pos].toInt() and 0xff
        pos += 1
        result = result or ((byte and 0x7f).toLong() shl shift)
        if (byte and 0x80 == 0) return Cursor(result, pos)
        shift += 7
    }
    throw SocketException("битый ответ телевизора")
}

private fun writeFrame(output: OutputStream, message: ByteArray) {
    var size = message.size
    val prefix = ByteArrayOutputStream()
    while (true) {
        val bits = size and 0x7f
        size = size ushr 7
        if (size == 0) {
            prefix.write(bits)
            break
        }
        prefix.write(bits or 0x80)
    }
    output.write(prefix.toByteArray())
    output.write(message)
    output.flush()
}

private fun readFrame(input: InputStream): ByteArray {
    var size = 0L
    var shift = 0
    while (shift <= 63) {
        val byte = input.read()
        if (byte < 0) throw SocketException("телевизор закрыл соединение")
        size = size or ((byte and 0x7f).toLong() shl shift)
        if (byte and 0x80 == 0) break
        shift += 7
    }
    if (size < 0 || size > 1_048_576) throw SocketException("битый ответ телевизора")
    val body = ByteArray(size.toInt())
    var read = 0
    while (read < body.size) {
        val got = input.read(body, read, body.size - read)
        if (got < 0) throw SocketException("телевизор закрыл соединение")
        read += got
    }
    return body
}

private fun hexBytes(text: String): ByteArray {
    val out = ByteArray(text.length / 2)
    for (index in out.indices) {
        out[index] = text.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
    return out
}

private fun pem(kind: String, body: ByteArray): String {
    val encoded = Base64.getMimeEncoder(64, byteArrayOf('\n'.code.toByte())).encodeToString(body)
    return "-----BEGIN $kind-----\n$encoded\n-----END $kind-----\n"
}

private fun decodePem(text: String): ByteArray {
    val body = text.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("-----") }
        .joinToString("")
    return Base64.getDecoder().decode(body)
}

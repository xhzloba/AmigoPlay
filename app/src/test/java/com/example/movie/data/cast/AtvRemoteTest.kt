package com.example.movie.data.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigInteger
import java.security.interfaces.RSAPublicKey

class AtvRemoteTest {
    @Test
    fun launchCarriesTheStreamLink() {
        val link = "vlc://http://192.168.1.20:8765/p/abc.m3u8"
        val fields = parseProto(launchFrame(link))
        val nested = parseProto(fields.blob(90)!!)
        assertEquals(link, nested.blob(1)!!.toString(Charsets.UTF_8))
    }

    @Test
    fun pinHashAcceptsOnlyTheShownPrefix() {
        val modulus = BigInteger("ff".repeat(32), 16)
        val exponent = BigInteger.valueOf(65537)
        val suffix = "BEEF"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(unsigned(modulus))
        digest.update(unsigned(exponent))
        digest.update(unsigned(modulus))
        digest.update(unsigned(exponent))
        digest.update(byteArrayOf(0xBE.toByte(), 0xEF.toByte()))
        val prefix = "%02X".format(digest.digest()[0])
        assertNotNull(pinHash(modulus, exponent, modulus, exponent, prefix + suffix))
        val wrong = if (prefix == "00") "01" else "00"
        assertNull(pinHash(modulus, exponent, modulus, exponent, wrong + suffix))
        assertNull(pinHash(modulus, exponent, modulus, exponent, "ABC"))
    }

    @Test
    fun unsignedDropsTheSignByte() {
        val value = BigInteger("80", 16)
        assertEquals(1, unsigned(value).size)
        assertEquals(0x80.toByte(), unsigned(value)[0])
    }

    @Test
    fun identityReloadsTheSameKey() {
        val directory = File.createTempFile("atv", "dir")
        directory.delete()
        directory.mkdirs()
        val created = AtvIdentity.load(directory)
        val loaded = AtvIdentity.load(directory)
        val first = created.certificate.publicKey as RSAPublicKey
        val second = loaded.certificate.publicKey as RSAPublicKey
        assertEquals(first.modulus, second.modulus)
        assertTrue(created.certificate.subjectX500Principal.name.contains("AmigoPlay"))
        directory.deleteRecursively()
    }
}

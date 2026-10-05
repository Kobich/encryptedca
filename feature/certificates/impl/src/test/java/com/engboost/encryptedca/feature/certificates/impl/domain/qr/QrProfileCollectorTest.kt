package com.engboost.encryptedca.feature.certificates.impl.domain.qr

import com.engboost.encryptedca.feature.certificates.api.entity.QrCollectResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64

class QrProfileCollectorTest {
    private val p12 = ByteArray(300) { it.toByte() }
    private val ca = ByteArray(200) { (it * 7).toByte() }
    private val collector = QrProfileCollector()

    @Test
    fun codesInAnyOrderAssembleTheProfile() {
        val codes = codesOf(payloadOf(p12, ca)).reversed()

        val results = codes.map { collector.add(listOf(it)) }

        assertEquals(List(3) { QrCollectResult.Collecting } + QrCollectResult.Complete, results)
        assertArrayEquals(p12, collector.profile!!.p12)
        assertArrayEquals(ca, collector.profile!!.caCertificate)
    }

    @Test
    fun severalCodesInOneFrameAreAllRead() {
        assertEquals(QrCollectResult.Complete, collector.add(codesOf(payloadOf(p12, ca))))
    }

    @Test
    fun unrelatedCodesAndCodesOfAnotherProfileAreIgnored() {
        collector.add(listOf(codesOf(payloadOf(p12, ca)).first()))

        val result = collector.add(listOf("https://example.com", codesOf(payloadOf(p12, ca), id = "ffffffff")[1]))

        assertEquals(QrCollectResult.NothingNew, result)
        assertEquals(1, collector.received)
    }

    @Test
    fun repeatedCodeIsNothingNew() {
        val first = codesOf(payloadOf(p12, ca)).first()
        collector.add(listOf(first))

        assertEquals(QrCollectResult.NothingNew, collector.add(listOf(first)))
    }

    @Test
    fun wrongChecksumIsInvalidAndCollectingStartsOver() {
        val payload = payloadOf(p12, ca).also { it[it.lastIndex] = (it.last() + 1).toByte() }

        assertTrue(collector.add(codesOf(payload)) is QrCollectResult.Invalid)
        assertEquals(0, collector.received)
        assertEquals(QrCollectResult.Complete, collector.add(codesOf(payloadOf(p12, ca))))
    }

    @Test
    fun blockLengthBeyondThePayloadIsInvalid() {
        val body = ByteBuffer.allocate(8).putInt(1_000).putInt(0).array()

        assertTrue(collector.add(codesOf(withChecksum(body), parts = 1)) is QrCollectResult.Invalid)
    }

    @Test
    fun textThatIsNotBase64IsInvalid() {
        assertTrue(collector.add(listOf("ECA1:0a1b2c3d:1/1:A")) is QrCollectResult.Invalid)
    }

    @Test
    fun clearWipesTheCollectedProfile() {
        collector.add(codesOf(payloadOf(p12, ca)))
        val profile = collector.profile!!

        collector.clear()

        assertNull(collector.profile)
        assertTrue(profile.p12.all { it == 0.toByte() })
        assertTrue(profile.caCertificate.all { it == 0.toByte() })
    }

    private fun payloadOf(p12: ByteArray, ca: ByteArray): ByteArray {
        val body = ByteBuffer.allocate(8 + p12.size + ca.size).putInt(p12.size).put(p12).putInt(ca.size).put(ca).array()
        return withChecksum(body)
    }

    private fun withChecksum(body: ByteArray) = body + MessageDigest.getInstance("SHA-256").digest(body)

    private fun codesOf(payload: ByteArray, id: String = "0a1b2c3d", parts: Int = 4): List<String> {
        val base64 = Base64.getEncoder().encodeToString(payload)
        return base64.chunked((base64.length + parts - 1) / parts)
            .mapIndexed { i, chunk -> "ECA1:$id:${i + 1}/$parts:$chunk" }
    }
}

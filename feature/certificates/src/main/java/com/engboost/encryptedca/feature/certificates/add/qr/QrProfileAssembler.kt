package com.engboost.encryptedca.feature.certificates.add.qr

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64

/** The client .p12 and the CA read from a set of QR codes; the format is in docs/qr-profile-format.md. */
internal class QrProfile(val p12: ByteArray, val caCertificate: ByteArray) {
    fun wipe() {
        p12.fill(0)
        caCertificate.fill(0)
    }
}

internal class QrFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Collects the codes of one profile in any order, several per camera frame if they fit.
 * The first valid code fixes the profile id; codes of other profiles and unrelated QR codes are ignored.
 */
internal class QrProfileAssembler {
    private var profileId: String? = null
    private var parts: Array<String?> = emptyArray()

    val received: Int get() = parts.count { it != null }
    val total: Int get() = parts.size
    val complete: Boolean get() = parts.isNotEmpty() && parts.all { it != null }

    /** Returns whether [text] was a new part of the profile being collected. */
    fun add(text: String): Boolean {
        val part = QrPart.parse(text) ?: return false
        if (profileId == null) {
            profileId = part.profileId
            parts = arrayOfNulls(part.total)
        }
        if (part.profileId != profileId || part.total != parts.size || parts[part.index] != null) return false
        parts[part.index] = part.data
        return true
    }

    /** Joins the parts once [complete]; throws [QrFormatException] when they don't form a valid profile. */
    fun assemble(): QrProfile {
        check(complete) { "Not all parts are collected" }
        val payload = try {
            Base64.getDecoder().decode(parts.joinToString(""))
        } catch (e: IllegalArgumentException) {
            throw QrFormatException("Payload is not base64", e)
        }
        return decodePayload(payload)
    }

    fun reset() {
        profileId = null
        parts = emptyArray()
    }
}

/** One code: `ECA1:<id>:<number>/<total>:<base64 part>`, numbered from 1. */
private class QrPart(val profileId: String, val index: Int, val total: Int, val data: String) {
    companion object {
        private val FORMAT = Regex("""ECA1:([0-9a-fA-F]{8}):(\d{1,2})/(\d{1,2}):([A-Za-z0-9+/=]+)""")

        fun parse(text: String): QrPart? {
            val match = FORMAT.matchEntire(text.trim()) ?: return null
            val (id, number, total, data) = match.destructured
            val index = number.toInt() - 1
            val count = total.toInt()
            if (count == 0 || index !in 0 until count) return null
            return QrPart(id.lowercase(), index, count, data)
        }
    }
}

/** `u32 p12 length | p12 | u32 CA length | CA | SHA-256 of everything before`. */
private fun decodePayload(payload: ByteArray): QrProfile {
    if (payload.size < HASH_SIZE) throw QrFormatException("Payload is too short")
    val body = payload.copyOfRange(0, payload.size - HASH_SIZE)
    val hash = payload.copyOfRange(payload.size - HASH_SIZE, payload.size)
    if (!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(body), hash)) {
        throw QrFormatException("Checksum doesn't match")
    }
    val buffer = ByteBuffer.wrap(body)
    val p12 = buffer.readBlock()
    val ca = buffer.readBlock()
    if (buffer.hasRemaining()) throw QrFormatException("Unexpected data after the CA")
    return QrProfile(p12, ca)
}

private fun ByteBuffer.readBlock(): ByteArray {
    if (remaining() < Int.SIZE_BYTES) throw QrFormatException("Block length is missing")
    val length = int
    if (length <= 0 || length > remaining()) throw QrFormatException("Invalid block length")
    return ByteArray(length).also { get(it) }
}

private const val HASH_SIZE = 32

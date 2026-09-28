package com.engboost.encryptedca.core.network

import android.net.Network
import java.net.Inet4Address
import java.net.InetAddress

/** The phone's IPv4 address in a Wi-Fi network. Sockets to its hosts must be created through [network]. */
class LocalNetwork internal constructor(
    internal val network: Network,
    val address: Inet4Address,
    val prefixLength: Int,
) {
    override fun toString() = "${address.hostAddress}/$prefixLength"

    /** Other hosts of the subnet. A subnet wider than /24 is narrowed to the phone's own /24. */
    internal fun hosts(): List<Inet4Address> {
        val size = 1u shl (32 - maxOf(prefixLength, MIN_SCANNED_PREFIX))
        val own = address.toUInt()
        val first = own and (size - 1u).inv()
        return (1u until size - 1u).map { first + it }.filter { it != own }.map { it.toInet4Address() }
    }

    private companion object {
        const val MIN_SCANNED_PREFIX = 24
    }
}

enum class DeviceStatus {
    /** The mTLS handshake with the active profile succeeded. */
    TRUSTED,
    /** Port 443 is open, but the handshake failed. */
    UNTRUSTED,
    /** The host is up, but port 443 is closed. */
    NO_TLS,
}

/** [serverFingerprint] is the SHA-256 of the certificate a [DeviceStatus.TRUSTED] device presented. */
data class FoundDevice(
    val address: Inet4Address,
    val status: DeviceStatus,
    val serverFingerprint: String? = null,
) : Comparable<FoundDevice> {
    val ip: String get() = address.hostAddress.orEmpty()

    override fun compareTo(other: FoundDevice) = address.toUInt().compareTo(other.address.toUInt())
}

private fun Inet4Address.toUInt(): UInt = address.fold(0u) { acc, byte -> (acc shl 8) or byte.toUByte().toUInt() }

private fun UInt.toInet4Address(): Inet4Address =
    InetAddress.getByAddress(ByteArray(4) { i -> (this shr (24 - 8 * i)).toByte() }) as Inet4Address

// The result of checking a host:
// TRUSTED - the mTLS handshake completed and the server certificate chains to the profile's CA;
// HANDSHAKE_FAILED - the port is open, but the handshake didn't complete;
// PORT_UNREACHABLE - the host is up, but the port couldn't be reached.
// serverFingerprint is the SHA-256 of the certificate (DER) a trusted device presented, as 64 lowercase hex chars.
// It isn't secret: the certificate itself is public and sent in every handshake.
// The web panel uses it to accept only this exact certificate from the device.
package com.engboost.encryptedca.core.network.api.entity

import java.net.Inet4Address

enum class DeviceStatus {
    TRUSTED,
    HANDSHAKE_FAILED,
    PORT_UNREACHABLE,
}

data class FoundDevice(
    val address: Inet4Address,
    val status: DeviceStatus,
    val serverFingerprint: String? = null,
) : Comparable<FoundDevice> {
    val ip: String get() = address.hostAddress.orEmpty()

    override fun compareTo(other: FoundDevice) = numericAddress.compareTo(other.numericAddress)

    private val numericAddress: Long
        get() = address.address.fold(0L) { value, octet ->
            (value shl 8) or (octet.toLong() and 0xFF)
        }
}

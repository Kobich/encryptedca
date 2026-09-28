package com.engboost.encryptedca.core.network.scan

import java.net.Inet4Address

/** What the probe actually established about a host; see [DeviceProbe]. */
enum class DeviceStatus {
    /** The TLS handshake with the active profile completed and the server certificate chains to its CA. */
    TRUSTED,
    /**
     * The port accepted a TCP connection, but the handshake didn't complete: an untrusted certificate,
     * a rejected client key, a timeout or a service that doesn't speak TLS.
     */
    HANDSHAKE_FAILED,
    /** The host is up (it refused the connection or answered ping), but no TCP connection to the port was made. */
    PORT_UNREACHABLE,
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

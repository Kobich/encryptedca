// Addresses to scan: the phone's whole subnet except the network address, the broadcast and the phone itself.
// A subnet wider than /24 is cut down to the phone's /24, otherwise a scan would take minutes.
package com.engboost.encryptedca.core.network.impl.domain

import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import com.engboost.encryptedca.core.network.api.toUInt
import java.net.Inet4Address
import java.net.InetAddress

private const val WIDEST_SCANNED_PREFIX = 24 // at most 254 hosts per scan
private const val IPV4_BITS = 32 // an IPv4 address is a 32-bit number
private const val IPV4_BYTES = 4 // and is written as 4 bytes
private const val BITS_PER_BYTE = 8

internal fun LocalNetwork.hostsToScan(): List<Inet4Address> {
    val prefix = maxOf(prefixLength, WIDEST_SCANNED_PREFIX)
    val subnetSize = 1u shl (IPV4_BITS - prefix) // 1u shl n = 2^n: 256 addresses for /24
    val ownAddress = address.toUInt()
    val networkAddress = ownAddress and (subnetSize - 1u).inv()
    val broadcastAddress = networkAddress + subnetSize - 1u
    return (networkAddress + 1u until broadcastAddress)
        .filter { it != ownAddress }
        .map { it.toInet4Address() }
}

private fun UInt.toInet4Address(): Inet4Address {
    val bytes = ByteArray(IPV4_BYTES) { i ->
        (this shr (IPV4_BITS - BITS_PER_BYTE * (i + 1))).toByte() // shifts 24, 16, 8, 0: highest byte first
    }
    return InetAddress.getByAddress(bytes) as Inet4Address
}

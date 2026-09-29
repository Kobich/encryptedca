// Addresses to scan: the phone's whole subnet except the network address, the broadcast and the phone itself.
// A subnet wider than /24 is cut down to the phone's /24, otherwise a scan would take minutes.
package com.engboost.encryptedca.core.network.impl.scan

import com.engboost.encryptedca.core.network.api.scan.toUInt
import com.engboost.encryptedca.core.network.api.wifi.LocalNetwork
import java.net.Inet4Address
import java.net.InetAddress

private const val WIDEST_SCANNED_PREFIX = 24

internal fun LocalNetwork.hostsToScan(): List<Inet4Address> {
    val prefix = maxOf(prefixLength, WIDEST_SCANNED_PREFIX)
    val subnetSize = 1u shl (32 - prefix)
    val ownAddress = address.toUInt()
    val networkAddress = ownAddress and (subnetSize - 1u).inv()
    val broadcastAddress = networkAddress + subnetSize - 1u
    return (networkAddress + 1u until broadcastAddress)
        .filter { it != ownAddress }
        .map { it.toInet4Address() }
}

private fun UInt.toInet4Address(): Inet4Address =
    InetAddress.getByAddress(ByteArray(4) { i -> (this shr (24 - 8 * i)).toByte() }) as Inet4Address

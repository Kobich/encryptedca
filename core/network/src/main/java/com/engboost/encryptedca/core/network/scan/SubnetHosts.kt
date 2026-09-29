// Адреса для сканирования: вся подсеть телефона, кроме адреса сети, broadcast и самого телефона.
// Подсеть шире /24 урезается до /24 телефона, иначе скан шёл бы минуты.
package com.engboost.encryptedca.core.network.scan

import com.engboost.encryptedca.core.network.wifi.LocalNetwork
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

internal fun Inet4Address.toUInt(): UInt =
    address.fold(0u) { acc, byte -> (acc shl 8) or byte.toUByte().toUInt() }

private fun UInt.toInet4Address(): Inet4Address =
    InetAddress.getByAddress(ByteArray(4) { i -> (this shr (24 - 8 * i)).toByte() }) as Inet4Address

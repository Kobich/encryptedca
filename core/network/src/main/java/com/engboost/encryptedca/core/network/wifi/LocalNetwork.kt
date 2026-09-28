package com.engboost.encryptedca.core.network.wifi

import android.net.Network
import java.net.Inet4Address

/**
 * The phone's IPv4 address in a Wi-Fi network. Sockets to its hosts must be created through [network].
 * Two values are equal only for the same Android network, address and prefix.
 */
data class LocalNetwork(
    val network: Network,
    val address: Inet4Address,
    val prefixLength: Int,
)

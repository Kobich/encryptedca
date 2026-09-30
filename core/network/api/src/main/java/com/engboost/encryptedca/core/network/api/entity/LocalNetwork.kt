package com.engboost.encryptedca.core.network.api.entity

import java.net.Inet4Address

// networkHandle is android.net.Network.getNetworkHandle(): sockets are bound to this network.
data class LocalNetwork(
    val networkHandle: Long,
    val address: Inet4Address,
    val prefixLength: Int,
)

package com.engboost.encryptedca.core.network.api.entity

import android.net.Network
import java.net.Inet4Address

data class LocalNetwork(
    val network: Network,
    val address: Inet4Address,
    val prefixLength: Int,
)

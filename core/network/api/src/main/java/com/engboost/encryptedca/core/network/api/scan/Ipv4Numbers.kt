package com.engboost.encryptedca.core.network.api.scan

import java.net.Inet4Address

fun Inet4Address.toUInt(): UInt =
    address.fold(0u) { acc, byte -> (acc shl 8) or byte.toUByte().toUInt() }

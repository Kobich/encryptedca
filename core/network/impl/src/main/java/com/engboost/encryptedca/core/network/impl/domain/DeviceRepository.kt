package com.engboost.encryptedca.core.network.impl.domain

import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import java.net.Inet4Address
import javax.net.ssl.SSLContext

internal interface DeviceRepository {
    fun probe(networkHandle: Long, sslContext: SSLContext, host: Inet4Address): FoundDevice?
}

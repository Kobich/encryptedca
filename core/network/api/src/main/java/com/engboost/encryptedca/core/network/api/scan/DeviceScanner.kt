package com.engboost.encryptedca.core.network.api.scan

import com.engboost.encryptedca.core.network.api.wifi.LocalNetwork
import kotlinx.coroutines.flow.Flow
import javax.net.ssl.SSLContext

interface DeviceScanner {
    fun scan(wifi: LocalNetwork, sslContext: SSLContext): Flow<FoundDevice>
}

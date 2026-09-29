// Проверяет все хосты подсети Wi-Fi через DeviceProbe, до 64 одновременно.
// Каждый живой хост отдаётся сразу после проверки, не дожидаясь остальных.
package com.engboost.encryptedca.core.network.scan

import com.engboost.encryptedca.core.network.wifi.LocalNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.net.ssl.SSLContext

class DeviceScanner {
    fun scan(wifi: LocalNetwork, sslContext: SSLContext, port: Int = HTTPS_PORT): Flow<FoundDevice> = channelFlow {
        val probe = DeviceProbe(wifi.network, sslContext, port)
        val permits = Semaphore(PARALLEL_PROBES)
        wifi.hostsToScan().forEach { host ->
            launch {
                permits.withPermit { probe.check(host)?.let { send(it) } }
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val HTTPS_PORT = 443
        const val PARALLEL_PROBES = 64
    }
}

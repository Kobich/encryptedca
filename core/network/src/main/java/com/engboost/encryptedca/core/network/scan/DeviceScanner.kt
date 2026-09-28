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

/** Checks every host of a Wi-Fi subnet with [DeviceProbe], several at a time. */
class DeviceScanner {

    /** Emits each live host as soon as it has been checked; hosts that don't answer are skipped. */
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

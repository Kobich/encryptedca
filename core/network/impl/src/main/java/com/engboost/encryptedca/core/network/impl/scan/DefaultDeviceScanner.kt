// Checks every host of the Wi-Fi subnet with DeviceProbe, up to 64 at a time.
// Each live host is emitted as soon as it is checked, without waiting for the rest.
package com.engboost.encryptedca.core.network.impl.scan

import com.engboost.encryptedca.core.network.api.scan.DeviceScanner
import com.engboost.encryptedca.core.network.api.scan.FoundDevice
import com.engboost.encryptedca.core.network.api.wifi.LocalNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.net.ssl.SSLContext

internal class DefaultDeviceScanner : DeviceScanner {
    override fun scan(wifi: LocalNetwork, sslContext: SSLContext): Flow<FoundDevice> = channelFlow {
        val probe = DeviceProbe(wifi.network, sslContext, HTTPS_PORT)
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

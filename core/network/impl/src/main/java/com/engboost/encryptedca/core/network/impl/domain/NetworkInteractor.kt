// Scans a Wi-Fi subnet: probes every host through DeviceRepository, up to 64 at a time.
package com.engboost.encryptedca.core.network.impl.domain

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.net.ssl.SSLContext

internal class NetworkInteractor(
    private val wifiRepository: WifiRepository,
    private val deviceRepository: DeviceRepository,
    private val sslContextRepository: SslContextRepository,
) {
    fun observeWifi(): Flow<LocalNetwork?> = wifiRepository.observeNetwork()

    fun scan(wifi: LocalNetwork, sslContext: SSLContext): Flow<FoundDevice> = channelFlow {
        val permits = Semaphore(PARALLEL_PROBES)
        wifi.hostsToScan().forEach { host ->
            launch {
                permits.withPermit { deviceRepository.probe(wifi.network, sslContext, host)?.let { send(it) } }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun createSslContext(credentials: ClientCredentials): SSLContext = sslContextRepository.create(credentials)

    private companion object {
        const val PARALLEL_PROBES = 64
    }
}

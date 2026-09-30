package com.engboost.encryptedca.core.network.impl

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.core.network.api.NetworkFeature
import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import com.engboost.encryptedca.core.network.impl.domain.NetworkInteractor
import kotlinx.coroutines.flow.Flow
import javax.net.ssl.SSLContext

internal class NetworkFeatureImpl(
    private val interactor: NetworkInteractor,
) : NetworkFeature {
    override fun observeWifi(): Flow<LocalNetwork?> = interactor.observeWifi()

    override fun scan(wifi: LocalNetwork, sslContext: SSLContext): Flow<FoundDevice> = interactor.scan(wifi, sslContext)

    override fun createSslContext(credentials: ClientCredentials): SSLContext = interactor.createSslContext(credentials)
}

// The phone's Wi-Fi network and the devices in it.
// observeWifi() emits the current Wi-Fi network, or null while there is none.
// scan() checks every host of the network over mTLS and emits each live one as soon as it is checked.
// createSslContext() throws TlsSetupException when the profile's key or CA can't be used.
package com.engboost.encryptedca.core.network.api

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import kotlinx.coroutines.flow.Flow
import javax.net.ssl.SSLContext

interface NetworkFeature {
    fun observeWifi(): Flow<LocalNetwork?>

    fun scan(wifi: LocalNetwork, sslContext: SSLContext): Flow<FoundDevice>

    fun createSslContext(credentials: ClientCredentials): SSLContext
}

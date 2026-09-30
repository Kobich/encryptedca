// SSLContext for mTLS: connects with the profile's key and trusts only servers
// whose certificate chains to the profile's CA.
package com.engboost.encryptedca.core.network.impl.data

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.core.network.api.entity.TlsSetupException
import com.engboost.encryptedca.core.network.impl.domain.SslContextRepository
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

internal class SslContextRepositoryImpl : SslContextRepository {

    override fun create(credentials: ClientCredentials): SSLContext =
        try {
            val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null)
                setCertificateEntry("profile_ca", credentials.trustAnchor)
            }
            val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                .apply { init(trustStore) }
                .trustManagers
            val keyManager = ClientKeyManager(credentials.privateKey, credentials.certificateChain.toTypedArray())
            SSLContext.getInstance("TLS").apply { init(arrayOf(keyManager), trustManagers, null) }
        } catch (e: Exception) {
            throw TlsSetupException("Could not create SSLContext for profile ${credentials.profileId}", e)
        }
}

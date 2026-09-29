// SSLContext для mTLS: подключаемся к устройству с ключом профиля и доверяем только серверам,
// чей сертификат ведёт к CA профиля.
package com.engboost.encryptedca.core.network.tls

import com.engboost.encryptedca.core.certificates.model.ClientCredentials
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class TlsSetupException(message: String, cause: Throwable) : Exception(message, cause)

fun ClientCredentials.createSslContext(): SSLContext =
    try {
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setCertificateEntry("profile_ca", trustAnchor)
        }
        val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(trustStore) }
            .trustManagers
        val keyManager = ClientKeyManager(privateKey, certificateChain.toTypedArray())
        SSLContext.getInstance("TLS").apply { init(arrayOf(keyManager), trustManagers, null) }
    } catch (e: Exception) {
        throw TlsSetupException("Could not create SSLContext for profile $profileId", e)
    }

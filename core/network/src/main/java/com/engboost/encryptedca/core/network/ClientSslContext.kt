package com.engboost.encryptedca.core.network

import com.engboost.encryptedca.core.certificates.ClientCredentials
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class TlsSetupException(message: String, cause: Throwable) : Exception(message, cause)

/**
 * Client-side mutual TLS: authenticates to the server with the profile's key and trusts only
 * servers whose certificate chains to the profile's CA.
 */
fun ClientCredentials.createSslContext(): SSLContext =
    try {
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setCertificateEntry("profile_ca", trustAnchor)
        }
        val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(trustStore) }
            .trustManagers
        SSLContext.getInstance("TLS").apply { init(arrayOf(keyManager), trustManagers, null) }
    } catch (e: Exception) {
        throw TlsSetupException("Could not create SSLContext for profile $profileId", e)
    }

/** Hex SHA-256 of the DER encoding: identifies the exact certificate a device presented. */
fun X509Certificate.sha256Fingerprint(): String =
    MessageDigest.getInstance("SHA-256").digest(encoded).joinToString("") { "%02x".format(it) }

package com.engboost.encryptedca.core.certificates

import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.X509ExtendedKeyManager

/**
 * What a TLS client needs from a profile. The key lives in Android Keystore: [privateKey] is a handle
 * that can sign but can't be exported, and [keyManager] presents only this profile's key.
 */
class ClientCredentials internal constructor(
    val profileId: String,
    val keyManager: X509ExtendedKeyManager,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
    val trustAnchor: X509Certificate,
)

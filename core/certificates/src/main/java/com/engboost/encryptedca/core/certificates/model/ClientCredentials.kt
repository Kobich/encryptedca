package com.engboost.encryptedca.core.certificates.model

import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * What a TLS client needs from a profile. [privateKey] is an Android Keystore handle:
 * it can sign but can't be exported.
 */
class ClientCredentials internal constructor(
    val profileId: String,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
    val trustAnchor: X509Certificate,
)

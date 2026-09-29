package com.engboost.encryptedca.core.certificates.model

import java.security.PrivateKey
import java.security.cert.X509Certificate

class ClientCredentials internal constructor(
    val profileId: String,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
    val trustAnchor: X509Certificate,
)

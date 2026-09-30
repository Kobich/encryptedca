package com.engboost.encryptedca.core.certificates.api.entity

import java.security.PrivateKey
import java.security.cert.X509Certificate

class ClientCredentials(
    val profileId: String,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
    val trustAnchor: X509Certificate,
)

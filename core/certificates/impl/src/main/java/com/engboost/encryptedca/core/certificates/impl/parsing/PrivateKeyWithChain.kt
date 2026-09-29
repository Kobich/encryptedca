package com.engboost.encryptedca.core.certificates.impl.parsing

import java.security.PrivateKey
import java.security.cert.X509Certificate

internal class PrivateKeyWithChain(
    val privateKey: PrivateKey,
    val certificateChain: Array<X509Certificate>,
)

package com.engboost.encryptedca.core.certificates.model

import java.security.PrivateKey
import java.security.cert.X509Certificate

/** A client private key with its certificate chain, leaf first; what a .p12 yields and Keystore stores. */
internal class PrivateKeyWithChain(
    val privateKey: PrivateKey,
    val certificateChain: Array<X509Certificate>,
)

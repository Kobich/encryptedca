// Everything needed for mTLS with one profile. Invalid credentials can't be created:
// the chain must not be empty and the key must be of the same type as the client certificate.
package com.engboost.encryptedca.core.certificates.api.entity

import java.security.PrivateKey
import java.security.cert.X509Certificate

class ClientCredentials(
    val profileId: String,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
    val trustAnchor: X509Certificate,
) {
    init {
        require(certificateChain.isNotEmpty()) { "Client certificate chain is empty" }
        val certificateKeyAlgorithm = certificateChain.first().publicKey.algorithm
        require(privateKey.algorithm.equals(certificateKeyAlgorithm, ignoreCase = true)) {
            "Private key is ${privateKey.algorithm}, client certificate key is $certificateKeyAlgorithm"
        }
    }
}

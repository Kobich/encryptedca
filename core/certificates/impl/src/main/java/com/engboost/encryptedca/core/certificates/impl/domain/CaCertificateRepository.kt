package com.engboost.encryptedca.core.certificates.impl.domain

import java.security.cert.X509Certificate

internal interface CaCertificateRepository {
    fun write(profileId: String, caCertificate: X509Certificate)

    fun read(profileId: String): X509Certificate

    fun exists(profileId: String): Boolean

    fun delete(profileId: String)
}

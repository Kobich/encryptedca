package com.engboost.encryptedca.core.certificates.api.entity

class CertificateProfileException(
    val error: CertificateProfileError,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

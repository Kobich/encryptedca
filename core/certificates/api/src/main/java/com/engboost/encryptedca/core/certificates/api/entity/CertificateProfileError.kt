package com.engboost.encryptedca.core.certificates.api.entity

enum class CertificateProfileError {
    FILE_UNAVAILABLE,
    PKCS12_PASSWORD_OR_CORRUPT,
    PKCS12_KEY_UNAVAILABLE,
    CERTIFICATE_INVALID,
    STORAGE_FAILED,
    PROFILE_INCOMPLETE,
}

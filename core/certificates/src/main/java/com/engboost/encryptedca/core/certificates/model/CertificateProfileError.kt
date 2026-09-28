package com.engboost.encryptedca.core.certificates.model

enum class CertificateProfileError {
    FILE_UNAVAILABLE,
    /** Providers don't distinguish a wrong password from a damaged or unsupported container. */
    PKCS12_PASSWORD_OR_CORRUPT,
    PKCS12_KEY_UNAVAILABLE,
    CERTIFICATE_INVALID,
    STORAGE_FAILED,
    /** The profile is in the index, but its key or CA file is missing. */
    PROFILE_INCOMPLETE,
}

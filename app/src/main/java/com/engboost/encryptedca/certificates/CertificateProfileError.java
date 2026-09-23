package com.engboost.encryptedca.certificates;

/** Error categories exposed to the import UI. Messages stay outside storage code. */
public enum CertificateProfileError {
    FILE_UNAVAILABLE,
    PKCS12_PASSWORD_OR_CORRUPT,
    CERTIFICATE_INVALID,
    STORAGE_FAILED,
    PROFILE_INCOMPLETE
}

package com.engboost.encryptedca.certificates;

/** Thrown when a certificate profile cannot be imported or read safely. */
public final class CertificateProfileException extends RuntimeException {
    private final CertificateProfileError error;

    public CertificateProfileException(CertificateProfileError error, String message) {
        super(message);
        this.error = error;
    }

    public CertificateProfileException(CertificateProfileError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public CertificateProfileError getError() { return error; }
}

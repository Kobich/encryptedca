package com.engboost.encryptedca.certificates;

/** Thrown when a certificate profile cannot be imported or read safely. */
public final class CertificateProfileException extends RuntimeException {
    public CertificateProfileException(String message) { super(message); }
    public CertificateProfileException(String message, Throwable cause) { super(message, cause); }
}

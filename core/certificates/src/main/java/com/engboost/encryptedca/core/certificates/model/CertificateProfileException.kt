package com.engboost.encryptedca.core.certificates.model

class CertificateProfileException(
    val error: CertificateProfileError,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

internal fun Throwable.asProfileException(error: CertificateProfileError, message: String): CertificateProfileException =
    this as? CertificateProfileException ?: CertificateProfileException(error, message, this)

internal inline fun <T> rethrowAs(error: CertificateProfileError, message: String, block: () -> T): T =
    try {
        block()
    } catch (e: Exception) {
        throw e.asProfileException(error, message)
    }

package com.engboost.encryptedca.core.certificates.impl

import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException

internal fun Throwable.asProfileException(error: CertificateProfileError, message: String): CertificateProfileException =
    this as? CertificateProfileException ?: CertificateProfileException(error, message, this)

internal inline fun <T> rethrowAs(error: CertificateProfileError, message: String, block: () -> T): T =
    try {
        block()
    } catch (e: Exception) {
        throw e.asProfileException(error, message)
    }

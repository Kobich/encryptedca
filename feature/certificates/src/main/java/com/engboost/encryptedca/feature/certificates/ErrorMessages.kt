package com.engboost.encryptedca.feature.certificates

import androidx.annotation.StringRes
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError

@StringRes
internal fun CertificateProfileError.messageRes(): Int = when (this) {
    CertificateProfileError.FILE_UNAVAILABLE -> R.string.error_file_unavailable
    CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT -> R.string.error_p12_invalid
    CertificateProfileError.PKCS12_KEY_UNAVAILABLE -> R.string.error_p12_key_unavailable
    CertificateProfileError.CERTIFICATE_INVALID -> R.string.error_certificate_invalid
    CertificateProfileError.STORAGE_FAILED -> R.string.error_storage
    CertificateProfileError.PROFILE_INCOMPLETE -> R.string.error_profile_incomplete
}

package com.engboost.encryptedca.feature.certificates.api.model

import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError

sealed interface ImportResult {
    data class Imported(val profileId: String) : ImportResult
    data class Failed(val error: CertificateProfileError, val cause: Throwable) : ImportResult
}

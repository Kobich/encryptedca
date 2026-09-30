package com.engboost.encryptedca.ui.addprofile.impl.ui.entity

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource

internal data class DocumentViewState(val name: String?)

internal data class QrProgressViewState(
    val received: Int,
    val total: Int,
    val photosWithoutNewCodes: Boolean,
)

@Immutable
internal sealed interface AddProfileViewState {
    data class QrCamera(val progress: QrProgressViewState) : AddProfileViewState

    data class QrPhotos(val progress: QrProgressViewState) : AddProfileViewState

    data class Form(
        val source: ProfileSource,
        val p12: DocumentViewState? = null,
        val ca: DocumentViewState? = null,
        val qrInvalid: Boolean = false,
        val importing: Boolean = false,
        val canImport: Boolean = false,
        val importError: CertificateProfileError? = null,
        val imported: Boolean = false,
    ) : AddProfileViewState
}

package com.engboost.encryptedca.ui.addprofile.impl.domain.entity

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource

internal data class PickedDocument(val uri: String, val name: String?)

internal sealed interface QrStatus {
    data class Collecting(
        val received: Int,
        val total: Int,
        val photosWithoutNewCodes: Boolean = false,
    ) : QrStatus

    data object Ready : QrStatus
    data object Invalid : QrStatus
}

internal sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Importing : ImportStatus
    data class Imported(val profileId: String) : ImportStatus
    data class Failed(val error: CertificateProfileError) : ImportStatus
}

internal data class AddProfileState(
    val source: ProfileSource,
    val p12: PickedDocument? = null,
    val ca: PickedDocument? = null,
    val qr: QrStatus? = null,
    val importStatus: ImportStatus = ImportStatus.Idle,
) {
    val importing: Boolean get() = importStatus == ImportStatus.Importing
    val canImport: Boolean
        get() = !importing && when (source) {
            ProfileSource.FILES -> p12 != null && ca != null
            ProfileSource.QR_CAMERA, ProfileSource.QR_PHOTOS -> qr == QrStatus.Ready
        }
}

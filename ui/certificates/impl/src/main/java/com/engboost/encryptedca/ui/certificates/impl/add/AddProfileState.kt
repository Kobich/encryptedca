package com.engboost.encryptedca.ui.certificates.impl.add

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError

@Immutable
internal data class PickedDocument(val uri: Uri, val name: String?)

@Immutable
internal sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Importing : ImportStatus
    data class Imported(val profileId: String) : ImportStatus
    data class Failed(val error: CertificateProfileError) : ImportStatus
}

@Immutable
internal sealed interface QrStatus {
    data class Collecting(
        val received: Int = 0,
        val total: Int = 0,
        val photosWithoutNewCodes: Boolean = false,
    ) : QrStatus

    data object Ready : QrStatus
    data object Invalid : QrStatus
}

@Immutable
internal data class AddProfileState(
    val source: ProfileSource,
    val p12: PickedDocument? = null,
    val ca: PickedDocument? = null,
    val qr: QrStatus? = null,
    val status: ImportStatus = ImportStatus.Idle,
) {
    val importing: Boolean get() = status == ImportStatus.Importing
    val canImport: Boolean
        get() = !importing && when (source) {
            ProfileSource.FILES -> p12 != null && ca != null
            ProfileSource.QR_CAMERA, ProfileSource.QR_PHOTOS -> qr == QrStatus.Ready
        }
}

package com.engboost.encryptedca.feature.certificates.add

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError

/** A picked document; [name] is `null` until the provider reports it, or when it doesn't. */
@Immutable
internal data class PickedDocument(val uri: Uri, val name: String?)

@Immutable
internal sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Importing : ImportStatus
    data class Imported(val profileId: String) : ImportStatus
    data class Failed(val error: CertificateProfileError) : ImportStatus
}

/** Reading the .p12 and the CA from QR codes, with the camera or from photos. */
@Immutable
internal sealed interface QrStatus {
    /**
     * Codes are being collected; [total] is 0 until the first code of the profile is read.
     * [photosWithoutNewCodes] is set when the last picked photos added nothing.
     */
    data class Collecting(
        val received: Int = 0,
        val total: Int = 0,
        val photosWithoutNewCodes: Boolean = false,
    ) : QrStatus

    data object Ready : QrStatus
    data object Invalid : QrStatus
}

/** [source] decides what is filled in: [p12] and [ca] for files, [qr] for QR codes. */
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

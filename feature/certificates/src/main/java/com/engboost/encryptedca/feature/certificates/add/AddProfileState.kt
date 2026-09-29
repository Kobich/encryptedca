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

/** Reading the .p12 and the CA from QR codes instead of two files. */
@Immutable
internal sealed interface QrStatus {
    /** The scanner is open; [total] is 0 until the first code of the profile is read. */
    data class Collecting(
        val received: Int = 0,
        val total: Int = 0,
        val imageWithoutNewCodes: Boolean = false,
    ) : QrStatus

    data object Ready : QrStatus
    data object Invalid : QrStatus
}

/** The certificates come either from the two picked files or, when [qr] is [QrStatus.Ready], from QR codes. */
@Immutable
internal data class AddProfileState(
    val p12: PickedDocument? = null,
    val ca: PickedDocument? = null,
    val qr: QrStatus? = null,
    val status: ImportStatus = ImportStatus.Idle,
) {
    val importing: Boolean get() = status == ImportStatus.Importing
    val canImport: Boolean get() = !importing && (qr == QrStatus.Ready || (p12 != null && ca != null))
}

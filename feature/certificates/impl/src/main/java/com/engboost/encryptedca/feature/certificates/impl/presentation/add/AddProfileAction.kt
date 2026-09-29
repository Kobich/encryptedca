package com.engboost.encryptedca.feature.certificates.impl.presentation.add

import android.net.Uri

sealed interface AddProfileAction {
    data class P12Picked(val uri: Uri) : AddProfileAction
    data class CaPicked(val uri: Uri) : AddProfileAction
    data class QrCodesScanned(val texts: List<String>) : AddProfileAction
    data class QrPhotosPicked(val uris: List<Uri>) : AddProfileAction
    data object CollectQrAgain : AddProfileAction
    class Import(val displayName: String, val password: CharArray) : AddProfileAction
}

package com.engboost.encryptedca.ui.addprofile.impl.ui.entity

internal data class AddProfileCallbacks(
    val onClose: () -> Unit,
    val onP12Picked: (uri: String) -> Unit,
    val onCaPicked: (uri: String) -> Unit,
    val onQrCodesScanned: (texts: List<String>) -> Unit,
    val onQrPhotosPicked: (uris: List<String>) -> Unit,
    val onCollectQrAgain: () -> Unit,
    val onImport: (displayName: String, password: CharArray) -> Unit,
)

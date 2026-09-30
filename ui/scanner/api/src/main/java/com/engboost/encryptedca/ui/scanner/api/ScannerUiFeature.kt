package com.engboost.encryptedca.ui.scanner.api

import androidx.compose.runtime.Composable

interface ScannerUiFeature {
    @Composable
    fun Content(
        onOpenCertificates: () -> Unit,
        onOpenDevice: (host: String, serverFingerprint: String) -> Unit,
    )
}

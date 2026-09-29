package com.engboost.encryptedca.feature.scanner.ui.api

import androidx.compose.runtime.Composable

interface ScannerUi {
    @Composable
    fun Content(
        onOpenCertificates: () -> Unit,
        onOpenDevice: (host: String, serverFingerprint: String) -> Unit,
    )
}

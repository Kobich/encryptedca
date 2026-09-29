package com.engboost.encryptedca.ui.certificates.api

import androidx.compose.runtime.Composable

interface CertificatesUi {
    @Composable
    fun Content(onBack: () -> Unit)
}

package com.engboost.encryptedca.ui.certificates.api

import androidx.compose.runtime.Composable

interface CertificatesUiFeature {
    @Composable
    fun Content(onBack: () -> Unit)
}

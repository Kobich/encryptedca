package com.engboost.encryptedca.feature.certificates.ui.api

import androidx.compose.runtime.Composable

interface CertificatesUi {
    @Composable
    fun Content(onBack: () -> Unit)
}

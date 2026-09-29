package com.engboost.encryptedca.feature.webpanel.ui.api

import androidx.compose.runtime.Composable

interface WebPanelUi {
    @Composable
    fun Content(host: String, serverFingerprint: String, onClose: () -> Unit)
}

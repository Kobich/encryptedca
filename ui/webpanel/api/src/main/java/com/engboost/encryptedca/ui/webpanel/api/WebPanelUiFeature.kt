package com.engboost.encryptedca.ui.webpanel.api

import androidx.compose.runtime.Composable

interface WebPanelUiFeature {
    @Composable
    fun Content(host: String, serverFingerprint: String, onClose: () -> Unit)
}

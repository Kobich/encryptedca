package com.engboost.encryptedca.ui.webpanel.impl.ui.entity

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import java.security.cert.X509Certificate

internal data class WebPanelCallbacks(
    val onClose: () -> Unit,
    val onReload: () -> Unit,
    val onPageStarted: (url: String) -> Unit,
    val onPageFinished: () -> Unit,
    val onDownloadRequested: (url: String) -> Unit,
    val onImageMessageShown: (ImageMessage) -> Unit,
    val onClientCertRequested: (host: String, answer: (ClientCredentials?) -> Unit) -> Unit,
    val isTrustedServer: (url: String, certificate: X509Certificate?) -> Boolean,
)

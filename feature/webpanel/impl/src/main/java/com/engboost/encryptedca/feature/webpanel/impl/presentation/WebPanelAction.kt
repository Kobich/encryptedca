package com.engboost.encryptedca.feature.webpanel.impl.presentation

import com.engboost.encryptedca.core.certificates.api.model.ClientCredentials
import java.security.cert.X509Certificate

sealed interface WebPanelAction {
    data object Reload : WebPanelAction
    data class PageStarted(val url: String) : WebPanelAction
    data object PageFinished : WebPanelAction
    data class DownloadRequested(val url: String) : WebPanelAction
    data class ImageMessageShown(val message: ImageMessage) : WebPanelAction

    class ClientCertRequested(
        val host: String,
        val answer: (ClientCredentials?) -> Unit,
    ) : WebPanelAction

    class ServerCertificateReceived(
        val url: String,
        val certificate: X509Certificate?,
        val answer: (trusted: Boolean) -> Unit,
    ) : WebPanelAction
}

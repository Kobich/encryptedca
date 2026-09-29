// Everything the device's web panel needs.
// The client key goes only to the device itself, never to pages it links to. It is read once per panel.
// WebView doesn't know the profile's CA, so a page is trusted only on the device's IP
// with exactly the certificate the scan verified over mTLS.
package com.engboost.encryptedca.feature.webpanel.api

import android.net.Uri
import com.engboost.encryptedca.feature.webpanel.api.model.ClientCertAnswer
import java.security.cert.X509Certificate

interface WebPanelInteractor {
    val host: String

    suspend fun answerClientCert(requestHost: String): ClientCertAnswer

    fun isTrustedServer(url: String, certificate: X509Certificate?): Boolean

    suspend fun saveScreenshot(dataUrl: String): Uri?
}

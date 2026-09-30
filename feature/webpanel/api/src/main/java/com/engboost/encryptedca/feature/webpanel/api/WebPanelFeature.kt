// What the device's web panel may do.
// The client key goes only to the device itself, never to pages it links to.
// WebView doesn't know the profile's CA, so a page is trusted only on the device's IP
// with exactly the certificate the scan verified over mTLS.
package com.engboost.encryptedca.feature.webpanel.api

import android.net.Uri
import com.engboost.encryptedca.feature.webpanel.api.entity.ClientCertAnswer
import com.engboost.encryptedca.feature.webpanel.api.entity.DevicePin
import java.security.cert.X509Certificate

interface WebPanelFeature {
    suspend fun answerClientCert(device: DevicePin, requestHost: String): ClientCertAnswer

    fun isTrustedServer(device: DevicePin, url: String, certificate: X509Certificate?): Boolean

    suspend fun saveScreenshot(dataUrl: String): Uri?
}

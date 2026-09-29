// The device a panel belongs to: its IP and the SHA-256 of the certificate the scan verified over mTLS.
// WebView doesn't know the profile's CA, so a page is trusted only on this host with exactly this certificate.
package com.engboost.encryptedca.feature.webpanel.impl.domain.model

import android.net.Uri
import com.engboost.encryptedca.core.network.api.tls.sha256Fingerprint
import java.security.cert.X509Certificate

class DevicePin(val host: String, val serverFingerprint: String) {

    fun isTrusted(url: String, certificate: X509Certificate?): Boolean =
        Uri.parse(url).host == host && certificate?.sha256Fingerprint() == serverFingerprint
}

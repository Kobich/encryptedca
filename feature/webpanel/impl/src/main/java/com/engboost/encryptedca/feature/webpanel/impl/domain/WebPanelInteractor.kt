package com.engboost.encryptedca.feature.webpanel.impl.domain

import android.net.Uri
import com.engboost.encryptedca.core.certificates.api.ProfileStorageFeature
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileException
import com.engboost.encryptedca.core.network.api.sha256Fingerprint
import com.engboost.encryptedca.feature.webpanel.api.entity.ClientCertAnswer
import com.engboost.encryptedca.feature.webpanel.api.entity.DevicePin
import java.security.cert.X509Certificate

internal class WebPanelInteractor(
    private val profileStorage: ProfileStorageFeature,
    private val screenshotRepository: ScreenshotRepository,
) {
    suspend fun answerClientCert(device: DevicePin, requestHost: String): ClientCertAnswer {
        if (requestHost != device.host) return ClientCertAnswer.OtherHost
        val credentials = try {
            profileStorage.loadActiveCredentials()
        } catch (e: CertificateProfileException) {
            return ClientCertAnswer.ProfileUnavailable
        }
        return credentials?.let(ClientCertAnswer::Granted) ?: ClientCertAnswer.NoProfile
    }

    fun isTrustedServer(device: DevicePin, url: String, certificate: X509Certificate?): Boolean =
        Uri.parse(url).host == device.host && certificate?.sha256Fingerprint() == device.serverFingerprint

    suspend fun saveScreenshot(dataUrl: String): Uri? = screenshotRepository.saveDataUrl(dataUrl)
}

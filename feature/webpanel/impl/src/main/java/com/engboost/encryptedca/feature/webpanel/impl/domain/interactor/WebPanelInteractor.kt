// Everything the device's web panel needs.
// The client key goes only to the device itself, never to pages it links to. It is read once per panel.
// A page loads only if DevicePin trusts the certificate the device showed.
package com.engboost.encryptedca.feature.webpanel.impl.domain.interactor

import android.net.Uri
import com.engboost.encryptedca.core.certificates.api.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.model.ClientCredentials
import com.engboost.encryptedca.feature.webpanel.impl.domain.model.ClientCertAnswer
import com.engboost.encryptedca.feature.webpanel.impl.domain.model.DevicePin
import com.engboost.encryptedca.feature.webpanel.impl.domain.repository.ScreenshotStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.cert.X509Certificate

internal class WebPanelInteractor(
    private val repository: CertificateProfileRepository,
    private val screenshots: ScreenshotStorage,
    private val pin: DevicePin,
) {
    private val credentialsLock = Mutex()
    private var credentials: Result<ClientCredentials?>? = null

    val host: String get() = pin.host

    suspend fun answerClientCert(requestHost: String): ClientCertAnswer {
        if (requestHost != pin.host) return ClientCertAnswer.OtherHost
        val result = credentialsLock.withLock { credentials ?: loadCredentials().also { credentials = it } }
        return result.fold(
            onSuccess = { loaded -> loaded?.let(ClientCertAnswer::Granted) ?: ClientCertAnswer.NoProfile },
            onFailure = { ClientCertAnswer.ProfileUnavailable },
        )
    }

    fun isTrustedServer(url: String, certificate: X509Certificate?): Boolean = pin.isTrusted(url, certificate)

    suspend fun saveScreenshot(dataUrl: String): Uri? = screenshots.saveDataUrl(dataUrl)

    private suspend fun loadCredentials(): Result<ClientCredentials?> =
        try {
            Result.success(repository.loadActiveCredentials())
        } catch (e: CertificateProfileException) {
            Result.failure(e)
        }
}

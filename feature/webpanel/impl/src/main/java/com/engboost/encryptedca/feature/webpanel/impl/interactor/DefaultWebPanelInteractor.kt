package com.engboost.encryptedca.feature.webpanel.impl.interactor

import android.net.Uri
import com.engboost.encryptedca.core.certificates.api.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.api.model.ClientCredentials
import com.engboost.encryptedca.core.network.api.tls.sha256Fingerprint
import com.engboost.encryptedca.feature.webpanel.api.WebPanelInteractor
import com.engboost.encryptedca.feature.webpanel.api.model.ClientCertAnswer
import com.engboost.encryptedca.feature.webpanel.api.model.DevicePin
import com.engboost.encryptedca.feature.webpanel.impl.data.ScreenshotStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.cert.X509Certificate

internal class DefaultWebPanelInteractor(
    private val repository: CertificateProfileRepository,
    private val screenshots: ScreenshotStorage,
    private val pin: DevicePin,
) : WebPanelInteractor {
    private val credentialsLock = Mutex()
    private var credentials: Result<ClientCredentials?>? = null

    override val host: String get() = pin.host

    override suspend fun answerClientCert(requestHost: String): ClientCertAnswer {
        if (requestHost != pin.host) return ClientCertAnswer.OtherHost
        val result = credentialsLock.withLock { credentials ?: loadCredentials().also { credentials = it } }
        return result.fold(
            onSuccess = { loaded -> loaded?.let(ClientCertAnswer::Granted) ?: ClientCertAnswer.NoProfile },
            onFailure = { ClientCertAnswer.ProfileUnavailable },
        )
    }

    override fun isTrustedServer(url: String, certificate: X509Certificate?): Boolean =
        Uri.parse(url).host == pin.host && certificate?.sha256Fingerprint() == pin.serverFingerprint

    override suspend fun saveScreenshot(dataUrl: String): Uri? = screenshots.saveDataUrl(dataUrl)

    private suspend fun loadCredentials(): Result<ClientCredentials?> =
        try {
            Result.success(repository.loadActiveCredentials())
        } catch (e: CertificateProfileException) {
            Result.failure(e)
        }
}

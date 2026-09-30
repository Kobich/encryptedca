// The device's web panel: the client certificate, the server check and screenshots.
// Every new load bumps loadAttempt, so a late answer for an earlier load can't report its problem on the new one.
package com.engboost.encryptedca.ui.webpanel.impl.domain

import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.feature.webpanel.api.WebPanelFeature
import com.engboost.encryptedca.feature.webpanel.api.entity.ClientCertAnswer
import com.engboost.encryptedca.feature.webpanel.api.entity.DevicePin
import com.engboost.encryptedca.ui.webpanel.impl.domain.entity.WebPanelProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.cert.X509Certificate

internal class WebPanelInteractor(
    private val webPanelFeature: WebPanelFeature,
    private val device: DevicePin,
) {
    val host: String = device.host
    val startUrl: String = "https://${device.host}:443/"

    private val _problem = MutableStateFlow<WebPanelProblem?>(null)
    val problem: StateFlow<WebPanelProblem?> = _problem.asStateFlow()

    private var loadAttempt = 0

    fun startNewLoad() {
        loadAttempt++
        _problem.value = null
    }

    suspend fun clientCredentials(requestHost: String): ClientCredentials? {
        val attempt = loadAttempt
        return when (val answer = webPanelFeature.answerClientCert(device, requestHost)) {
            is ClientCertAnswer.Granted -> answer.credentials
            ClientCertAnswer.OtherHost -> null
            ClientCertAnswer.NoProfile -> {
                report(attempt, WebPanelProblem.NO_PROFILE)
                null
            }
            ClientCertAnswer.ProfileUnavailable -> {
                report(attempt, WebPanelProblem.PROFILE_UNAVAILABLE)
                null
            }
        }
    }

    fun isTrustedServer(url: String, certificate: X509Certificate?): Boolean {
        val trusted = webPanelFeature.isTrustedServer(device, url, certificate)
        if (!trusted) report(loadAttempt, WebPanelProblem.UNTRUSTED_SERVER)
        return trusted
    }

    suspend fun saveScreenshot(dataUrl: String): String? = webPanelFeature.saveScreenshot(dataUrl)

    private fun report(attempt: Int, problem: WebPanelProblem) {
        if (attempt == loadAttempt) _problem.value = problem
    }
}

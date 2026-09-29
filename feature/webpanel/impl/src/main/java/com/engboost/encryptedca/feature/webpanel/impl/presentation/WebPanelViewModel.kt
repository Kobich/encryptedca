// The device's web panel. Every reload bumps loadAttempt, so a late answer for an earlier load
// can't show its error on the new one.
package com.engboost.encryptedca.feature.webpanel.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.api.model.ClientCredentials
import com.engboost.encryptedca.feature.webpanel.impl.domain.interactor.WebPanelInteractor
import com.engboost.encryptedca.feature.webpanel.impl.domain.model.ClientCertAnswer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.cert.X509Certificate

class WebPanelViewModel internal constructor(
    private val interactor: WebPanelInteractor,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WebPanelState(host = interactor.host, startUrl = "https://${interactor.host}:443/"),
    )
    val state: StateFlow<WebPanelState> = _state.asStateFlow()

    private var loadAttempt = 0
    private var nextMessageId = 0L

    fun onAction(action: WebPanelAction) {
        when (action) {
            WebPanelAction.Reload -> reload()
            is WebPanelAction.PageStarted -> _state.update { it.copy(pageUrl = action.url, loading = true) }
            WebPanelAction.PageFinished -> _state.update { it.copy(loading = false) }
            is WebPanelAction.DownloadRequested -> saveScreenshot(action.url)
            is WebPanelAction.ImageMessageShown -> clearImageMessage(action.message)
            is WebPanelAction.ClientCertRequested -> answerClientCert(action.host, action.answer)
            is WebPanelAction.ServerCertificateReceived -> action.answer(checkServer(action.url, action.certificate))
        }
    }

    private fun reload() {
        loadAttempt++
        _state.update { it.copy(problem = null, loading = true) }
    }

    private fun answerClientCert(requestHost: String, answer: (ClientCredentials?) -> Unit) {
        val attempt = loadAttempt
        viewModelScope.launch {
            when (val result = interactor.answerClientCert(requestHost)) {
                is ClientCertAnswer.Granted -> answer(result.credentials)
                ClientCertAnswer.OtherHost -> answer(null)
                ClientCertAnswer.NoProfile -> {
                    answer(null)
                    reportProblem(attempt, WebPanelProblem.NO_PROFILE)
                }
                ClientCertAnswer.ProfileUnavailable -> {
                    answer(null)
                    reportProblem(attempt, WebPanelProblem.PROFILE_UNAVAILABLE)
                }
            }
        }
    }

    private fun checkServer(url: String, certificate: X509Certificate?): Boolean {
        val trusted = interactor.isTrustedServer(url, certificate)
        if (!trusted) reportProblem(loadAttempt, WebPanelProblem.UNTRUSTED_SERVER)
        return trusted
    }

    private fun saveScreenshot(url: String) {
        viewModelScope.launch {
            val saved = interactor.saveScreenshot(url)
            val id = nextMessageId++
            val message = if (saved != null) ImageMessage.Saved(id, saved.toString()) else ImageMessage.Failed(id)
            _state.update { it.copy(imageMessage = message) }
        }
    }

    private fun clearImageMessage(message: ImageMessage) =
        _state.update { if (it.imageMessage == message) it.copy(imageMessage = null) else it }

    private fun reportProblem(attempt: Int, problem: WebPanelProblem) {
        if (attempt == loadAttempt) _state.update { it.copy(problem = problem, loading = false) }
    }
}

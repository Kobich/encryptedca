// Решает, что можно веб-панели устройства.
// Клиентский ключ отдаётся только самому устройству, страницы по ссылкам его не получают.
// WebView не знает CA профиля и сообщает о каждом устройстве как об ошибке SSL. Загрузка продолжается,
// только если устройство показало ровно тот сертификат, который скан проверил по mTLS на этом IP.
// Каждая перезагрузка увеличивает loadAttempt, чтобы запоздалый ответ прошлой загрузки не показал ошибку в новой.
package com.engboost.encryptedca.feature.webpanel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.model.ClientCredentials
import com.engboost.encryptedca.core.network.tls.sha256Fingerprint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.cert.X509Certificate

internal class WebPanelViewModel(
    repository: CertificateProfileRepository,
    private val images: ImageSaver,
    private val host: String,
    private val serverFingerprint: String,
) : ViewModel() {

    private val credentials: Deferred<Result<ClientCredentials?>> = viewModelScope.async {
        try {
            Result.success(repository.loadActiveCredentials())
        } catch (e: CertificateProfileException) {
            Result.failure(e)
        }
    }

    private val _state = MutableStateFlow(WebPanelState(host = host, startUrl = "https://$host:443/"))
    val state: StateFlow<WebPanelState> = _state.asStateFlow()

    private var loadAttempt = 0
    private var nextMessageId = 0L

    fun onAction(action: WebPanelAction) {
        when (action) {
            WebPanelAction.Reload -> reload()
            is WebPanelAction.PageStarted -> _state.update { it.copy(pageUrl = action.url, loading = true) }
            WebPanelAction.PageFinished -> _state.update { it.copy(loading = false) }
            is WebPanelAction.DownloadRequested -> saveImage(action.url)
            is WebPanelAction.ImageMessageShown -> clearImageMessage(action.message)
            is WebPanelAction.ClientCertRequested -> answerClientCert(action.host, action.answer)
            is WebPanelAction.ServerCertificateReceived -> action.answer(isTrustedServer(action.url, action.certificate))
        }
    }

    private fun reload() {
        loadAttempt++
        _state.update { it.copy(problem = null, loading = true) }
    }

    private fun answerClientCert(requestHost: String, answer: (ClientCredentials?) -> Unit) {
        if (requestHost != host) return answer(null)
        val attempt = loadAttempt
        viewModelScope.launch {
            val result = credentials.await()
            val loaded = result.getOrNull()
            answer(loaded)
            if (loaded == null) {
                reportProblem(attempt, if (result.isFailure) WebPanelProblem.PROFILE_UNAVAILABLE else WebPanelProblem.NO_PROFILE)
            }
        }
    }

    private fun isTrustedServer(url: String, certificate: X509Certificate?): Boolean {
        val trusted = Uri.parse(url).host == host && certificate?.sha256Fingerprint() == serverFingerprint
        if (!trusted) reportProblem(loadAttempt, WebPanelProblem.UNTRUSTED_SERVER)
        return trusted
    }

    private fun saveImage(url: String) {
        viewModelScope.launch {
            val saved = images.saveDataUrl(url)
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

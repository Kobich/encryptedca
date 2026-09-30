package com.engboost.encryptedca.ui.webpanel.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.api.entity.ClientCredentials
import com.engboost.encryptedca.ui.webpanel.impl.domain.WebPanelInteractor
import com.engboost.encryptedca.ui.webpanel.impl.ui.entity.ImageMessage
import com.engboost.encryptedca.ui.webpanel.impl.ui.entity.WebPanelViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.cert.X509Certificate

internal class WebPanelViewModel(
    private val interactor: WebPanelInteractor,
) : ViewModel() {

    private val _state = MutableStateFlow(WebPanelViewState(host = interactor.host, startUrl = interactor.startUrl))
    val state: StateFlow<WebPanelViewState> = _state.asStateFlow()

    private var nextMessageId = 0L

    init {
        interactor.problem
            .onEach { problem -> _state.update { it.copy(problem = problem, loading = it.loading && problem == null) } }
            .launchIn(viewModelScope)
    }

    fun reload() {
        interactor.startNewLoad()
        _state.update { it.copy(loading = true) }
    }

    fun onPageStarted(url: String) = _state.update { it.copy(pageUrl = url, loading = true) }

    fun onPageFinished() = _state.update { it.copy(loading = false) }

    fun answerClientCert(host: String, answer: (ClientCredentials?) -> Unit) {
        viewModelScope.launch { answer(interactor.clientCredentials(host)) }
    }

    fun isTrustedServer(url: String, certificate: X509Certificate?): Boolean =
        interactor.isTrustedServer(url, certificate)

    fun saveScreenshot(dataUrl: String) {
        viewModelScope.launch {
            val saved = interactor.saveScreenshot(dataUrl)
            val id = nextMessageId++
            val message = if (saved != null) ImageMessage.Saved(id, saved) else ImageMessage.Failed(id)
            _state.update { it.copy(imageMessage = message) }
        }
    }

    fun onImageMessageShown(message: ImageMessage) =
        _state.update { if (it.imageMessage == message) it.copy(imageMessage = null) else it }
}

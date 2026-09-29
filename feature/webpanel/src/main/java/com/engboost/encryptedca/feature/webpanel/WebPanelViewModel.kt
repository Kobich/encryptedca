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

/** Decides what the panel's WebView may do; [PanelWebView] carries the decisions out. */
internal class WebPanelViewModel(
    repository: CertificateProfileRepository,
    private val images: ImageSaver,
    private val host: String,
    private val serverFingerprint: String,
) : ViewModel() {

    /** Read once per panel. Success with `null` means no profile is selected; failure means it couldn't be read. */
    private val credentials: Deferred<Result<ClientCredentials?>> = viewModelScope.async {
        try {
            Result.success(repository.loadActiveCredentials())
        } catch (e: CertificateProfileException) {
            Result.failure(e)
        }
    }

    private val _state = MutableStateFlow(WebPanelState(host = host, startUrl = "https://$host:443/"))
    val state: StateFlow<WebPanelState> = _state.asStateFlow()

    /** Bumped by every reload, so a late answer for an earlier load can't put its problem on the new one. */
    private var loadAttempt = 0
    private var nextMessageId = 0L

    fun onReload() {
        loadAttempt++
        _state.update { it.copy(problem = null, loading = true) }
    }

    fun onPageStarted(url: String) = _state.update { it.copy(pageUrl = url, loading = true) }

    fun onPageFinished() = _state.update { it.copy(loading = false) }

    /**
     * Answers with the profile's credentials, or `null` to refuse. The key is only for the device:
     * a page it links to must not get a signature from it.
     */
    fun onClientCertRequest(requestHost: String, answer: (ClientCredentials?) -> Unit) {
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

    /**
     * WebView doesn't know the profile's CA and reports every device as an SSL error. Returns whether
     * to continue: only with the exact certificate the scan verified over mTLS on this IP.
     */
    fun onSslError(url: String, certificate: X509Certificate?): Boolean {
        val trusted = Uri.parse(url).host == host && certificate?.sha256Fingerprint() == serverFingerprint
        if (!trusted) reportProblem(loadAttempt, WebPanelProblem.UNTRUSTED_SERVER)
        return trusted
    }

    fun onDownload(url: String) {
        viewModelScope.launch {
            val saved = images.saveDataUrl(url)
            val id = nextMessageId++
            val message = if (saved != null) ImageMessage.Saved(id, saved.toString()) else ImageMessage.Failed(id)
            _state.update { it.copy(imageMessage = message) }
        }
    }

    /** Clears only [message]: a newer one that arrived while it was on screen stays. */
    fun onImageMessageShown(message: ImageMessage) =
        _state.update { if (it.imageMessage == message) it.copy(imageMessage = null) else it }

    private fun reportProblem(attempt: Int, problem: WebPanelProblem) {
        if (attempt == loadAttempt) _state.update { it.copy(problem = problem, loading = false) }
    }
}

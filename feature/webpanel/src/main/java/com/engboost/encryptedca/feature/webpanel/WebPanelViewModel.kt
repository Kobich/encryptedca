package com.engboost.encryptedca.feature.webpanel

import android.net.Uri
import android.net.http.SslError
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.network.tls.sha256Fingerprint
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class WebPanelProblem { NO_PROFILE, UNTRUSTED_SERVER }

@Immutable
internal data class WebPanelState(
    /** The device's IP: the only host the panel trusts and shows. */
    val host: String,
    val url: String,
    val loading: Boolean = true,
    val problem: WebPanelProblem? = null,
    /** Content URI of a just-saved screenshot, until the message about it has been shown. */
    val savedImage: String? = null,
    val imageSaveFailed: Boolean = false,
)

internal class WebPanelViewModel(
    repository: CertificateProfileRepository,
    private val images: ImageSaver,
    private val host: String,
    private val serverFingerprint: String,
) : ViewModel() {

    private val credentials = viewModelScope.async {
        try {
            repository.loadActiveCredentials()
        } catch (e: CertificateProfileException) {
            null
        }
    }

    private val _state = MutableStateFlow(WebPanelState(host = host, url = "https://$host:443/"))
    val state: StateFlow<WebPanelState> = _state.asStateFlow()

    fun onPageStarted(url: String) = _state.update { it.copy(url = url, loading = true) }

    fun onPageFinished() = _state.update { it.copy(loading = false) }

    /** The key is only for the device: a page it links to must not get a signature from it. */
    fun onClientCertRequest(request: ClientCertRequest) {
        if (request.host != host) return request.cancel()
        viewModelScope.launch {
            val credentials = credentials.await()
            if (credentials == null) {
                request.cancel()
                _state.update { it.copy(problem = WebPanelProblem.NO_PROFILE) }
            } else {
                request.proceed(credentials.privateKey, credentials.certificateChain.toTypedArray())
            }
        }
    }

    /**
     * WebView doesn't know the profile's CA and reports every device as an SSL error. The page is
     * accepted only with the exact certificate the scan verified over mTLS on this IP.
     */
    fun onSslError(handler: SslErrorHandler, error: SslError) {
        val sameHost = Uri.parse(error.url).host == host
        if (sameHost && error.certificate.x509Certificate?.sha256Fingerprint() == serverFingerprint) {
            handler.proceed()
        } else {
            handler.cancel()
            _state.update { it.copy(problem = WebPanelProblem.UNTRUSTED_SERVER, loading = false) }
        }
    }

    fun onDownload(url: String) {
        viewModelScope.launch {
            val saved = images.saveDataUrl(url)
            _state.update { if (saved != null) it.copy(savedImage = saved.toString()) else it.copy(imageSaveFailed = true) }
        }
    }

    fun onMessageShown() = _state.update { it.copy(savedImage = null, imageSaveFailed = false) }
}

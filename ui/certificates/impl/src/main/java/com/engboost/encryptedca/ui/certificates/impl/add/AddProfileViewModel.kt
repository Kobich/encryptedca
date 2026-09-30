// The add-profile screen. The source (files, camera or photos) is picked on the list and doesn't change.
// Picked documents are remembered and survive process death; QR codes live only in the QrCollection.
package com.engboost.encryptedca.ui.certificates.impl.add

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.api.wipe
import com.engboost.encryptedca.feature.certificates.api.CertificatesFeature
import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import com.engboost.encryptedca.feature.certificates.api.entity.QrCollectResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AddProfileViewModel(
    private val feature: CertificatesFeature,
    private val savedState: SavedStateHandle,
    source: ProfileSource,
) : ViewModel() {

    private val _state = MutableStateFlow(AddProfileState(source))
    val state: StateFlow<AddProfileState> = _state.asStateFlow()

    private val qrCollection = feature.newQrCollection()

    init {
        when (source) {
            ProfileSource.FILES -> {
                savedState.get<Uri>(KEY_P12)?.let(::selectP12)
                savedState.get<Uri>(KEY_CA)?.let(::selectCa)
            }
            ProfileSource.QR_CAMERA, ProfileSource.QR_PHOTOS -> startQrCollecting()
        }
    }

    fun onAction(action: AddProfileAction) {
        when (action) {
            is AddProfileAction.P12Picked -> selectP12(action.uri)
            is AddProfileAction.CaPicked -> selectCa(action.uri)
            is AddProfileAction.QrCodesScanned -> addQrCodes(action.texts)
            is AddProfileAction.QrPhotosPicked -> readQrPhotos(action.uris)
            AddProfileAction.CollectQrAgain -> startQrCollecting()
            is AddProfileAction.Import -> startImport(action.displayName, action.password)
        }
    }

    override fun onCleared() = qrCollection.clear()

    private fun selectP12(uri: Uri) = selectDocument(KEY_P12, uri) { state, document -> state.copy(p12 = document) }

    private fun selectCa(uri: Uri) = selectDocument(KEY_CA, uri) { state, document -> state.copy(ca = document) }

    private fun selectDocument(key: String, uri: Uri, show: (AddProfileState, PickedDocument) -> AddProfileState) {
        savedState[key] = uri
        _state.update { show(it, PickedDocument(uri, name = null)) }
        viewModelScope.launch {
            val name = feature.documentName(uri)
            if (savedState.get<Uri>(key) == uri) _state.update { show(it, PickedDocument(uri, name)) }
        }
    }

    private fun startQrCollecting() {
        qrCollection.clear()
        _state.update { it.copy(qr = collecting()) }
    }

    private fun addQrCodes(texts: List<String>) {
        if (_state.value.qr !is QrStatus.Collecting) return
        val status = qrCollection.add(texts).toStatus() ?: return
        _state.update { it.copy(qr = status) }
    }

    private fun readQrPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            val texts = feature.readQrCodes(uris)
            if (_state.value.qr !is QrStatus.Collecting) return@launch
            val status = qrCollection.add(texts).toStatus() ?: collecting(photosWithoutNewCodes = true)
            _state.update { it.copy(qr = status) }
        }
    }

    private fun startImport(displayName: String, password: CharArray) {
        val current = _state.value
        if (!current.canImport) {
            password.wipe()
            return
        }
        val name = displayName.trim().ifEmpty { null }
        val p12Uri = current.p12?.uri
        val caUri = current.ca?.uri
        _state.update { it.copy(status = ImportStatus.Importing) }
        viewModelScope.launch {
            val result = if (p12Uri != null && caUri != null) {
                feature.importFromFiles(name, p12Uri, caUri, password)
            } else {
                qrCollection.importProfile(name, password)
            }
            if (result is ImportResult.Failed) Log.w(TAG, "Import failed: ${result.error}", result.cause)
            _state.update { it.copy(status = result.toStatus()) }
        }
    }

    private fun collecting(photosWithoutNewCodes: Boolean = false) =
        QrStatus.Collecting(qrCollection.received, qrCollection.total, photosWithoutNewCodes)

    private fun QrCollectResult.toStatus(): QrStatus? = when (this) {
        QrCollectResult.NothingNew -> null
        QrCollectResult.Collecting -> collecting()
        QrCollectResult.Complete -> QrStatus.Ready
        is QrCollectResult.Invalid -> {
            Log.w(TAG, "QR codes don't form a profile", cause)
            QrStatus.Invalid
        }
    }

    private fun ImportResult.toStatus(): ImportStatus = when (this) {
        is ImportResult.Imported -> ImportStatus.Imported(profileId)
        is ImportResult.Failed -> ImportStatus.Failed(error)
    }

    private companion object {
        const val TAG = "AddProfile"
        const val KEY_P12 = "p12_uri"
        const val KEY_CA = "ca_uri"
    }
}

// The add-profile screen. The source (files, camera or photos) is picked on the list and doesn't change.
// Picked documents are remembered and survive process death; QR codes live only in AddProfileInteractor.
package com.engboost.encryptedca.ui.certificates.impl.add

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.api.wipe
import com.engboost.encryptedca.feature.certificates.api.AddProfileInteractor
import com.engboost.encryptedca.feature.certificates.api.model.ImportResult
import com.engboost.encryptedca.feature.certificates.api.model.ProfileSource
import com.engboost.encryptedca.feature.certificates.api.model.QrCollectResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AddProfileViewModel(
    private val interactor: AddProfileInteractor,
    private val savedState: SavedStateHandle,
    source: ProfileSource,
) : ViewModel() {

    private val _state = MutableStateFlow(AddProfileState(source))
    val state: StateFlow<AddProfileState> = _state.asStateFlow()

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

    override fun onCleared() = interactor.clearQr()

    private fun selectP12(uri: Uri) = selectDocument(KEY_P12, uri) { state, document -> state.copy(p12 = document) }

    private fun selectCa(uri: Uri) = selectDocument(KEY_CA, uri) { state, document -> state.copy(ca = document) }

    private fun selectDocument(key: String, uri: Uri, show: (AddProfileState, PickedDocument) -> AddProfileState) {
        savedState[key] = uri
        _state.update { show(it, PickedDocument(uri, name = null)) }
        viewModelScope.launch {
            val name = interactor.documentName(uri)
            if (savedState.get<Uri>(key) == uri) _state.update { show(it, PickedDocument(uri, name)) }
        }
    }

    private fun startQrCollecting() {
        interactor.clearQr()
        _state.update { it.copy(qr = collecting()) }
    }

    private fun addQrCodes(texts: List<String>) {
        if (_state.value.qr !is QrStatus.Collecting) return
        val status = interactor.addQrCodes(texts).toStatus() ?: return
        _state.update { it.copy(qr = status) }
    }

    private fun readQrPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            val texts = interactor.readQrCodes(uris)
            if (_state.value.qr !is QrStatus.Collecting) return@launch
            val status = interactor.addQrCodes(texts).toStatus() ?: collecting(photosWithoutNewCodes = true)
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
                interactor.importFromFiles(name, p12Uri, caUri, password)
            } else {
                interactor.importFromQr(name, password)
            }
            if (result is ImportResult.Failed) Log.w(TAG, "Import failed: ${result.error}", result.cause)
            _state.update { it.copy(status = result.toStatus()) }
        }
    }

    private fun collecting(photosWithoutNewCodes: Boolean = false) =
        QrStatus.Collecting(interactor.qrReceived, interactor.qrTotal, photosWithoutNewCodes)

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

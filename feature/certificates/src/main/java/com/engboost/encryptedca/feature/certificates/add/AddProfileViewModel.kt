// Экран добавления профиля. Источник (файлы, камера или фото) выбран на списке и не меняется.
// Файлы: выбранные документы запоминаются и переживают перезапуск процесса.
// QR: коды собирает QrProfileCollector, собранный профиль живёт только в памяти.
// Сам импорт делает ProfileImporter.
package com.engboost.encryptedca.feature.certificates.add

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.wipe
import com.engboost.encryptedca.feature.certificates.add.qr.QrImageReader
import com.engboost.encryptedca.feature.certificates.add.qr.QrProfileCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AddProfileViewModel(
    private val documents: DocumentReader,
    private val importer: ProfileImporter,
    private val qrImages: QrImageReader,
    private val savedState: SavedStateHandle,
    source: ProfileSource,
) : ViewModel() {

    private val _state = MutableStateFlow(AddProfileState(source))
    val state: StateFlow<AddProfileState> = _state.asStateFlow()

    private val qrCollector = QrProfileCollector()

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
            is AddProfileAction.Import -> importProfile(action.displayName, action.password)
        }
    }

    override fun onCleared() = qrCollector.clear()

    private fun selectP12(uri: Uri) = selectDocument(KEY_P12, uri) { state, document -> state.copy(p12 = document) }

    private fun selectCa(uri: Uri) = selectDocument(KEY_CA, uri) { state, document -> state.copy(ca = document) }

    private fun selectDocument(key: String, uri: Uri, show: (AddProfileState, PickedDocument) -> AddProfileState) {
        savedState[key] = uri
        _state.update { show(it, PickedDocument(uri, name = null)) }
        viewModelScope.launch {
            val name = documents.displayName(uri)
            if (savedState.get<Uri>(key) == uri) _state.update { show(it, PickedDocument(uri, name)) }
        }
    }

    private fun startQrCollecting() {
        qrCollector.clear()
        _state.update { it.copy(qr = qrCollector.progress()) }
    }

    private fun addQrCodes(texts: List<String>) {
        if (_state.value.qr !is QrStatus.Collecting) return
        val status = qrCollector.add(texts) ?: return
        _state.update { it.copy(qr = status) }
    }

    private fun readQrPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            val texts = uris.flatMap { qrImages.read(it) }
            if (_state.value.qr !is QrStatus.Collecting) return@launch
            val status = qrCollector.add(texts) ?: qrCollector.progress(photosWithoutNewCodes = true)
            _state.update { it.copy(qr = status) }
        }
    }

    private fun importProfile(displayName: String, password: CharArray) {
        val current = _state.value
        if (!current.canImport) {
            password.wipe()
            return
        }
        val name = displayName.trim().ifEmpty { null }
        val qrProfile = qrCollector.profile.takeIf { current.qr == QrStatus.Ready }
        val p12Uri = current.p12?.uri
        val caUri = current.ca?.uri
        _state.update { it.copy(status = ImportStatus.Importing) }
        viewModelScope.launch {
            val status = when {
                qrProfile != null -> importer.importQr(name, qrProfile, password)
                p12Uri != null && caUri != null -> importer.importFiles(name, p12Uri, caUri, password)
                else -> {
                    password.wipe()
                    ImportStatus.Failed(CertificateProfileError.STORAGE_FAILED)
                }
            }
            _state.update { it.copy(status = status) }
        }
    }

    private companion object {
        const val KEY_P12 = "p12_uri"
        const val KEY_CA = "ca_uri"
    }
}

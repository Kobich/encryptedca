package com.engboost.encryptedca.feature.certificates.add

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.wipe
import com.engboost.encryptedca.feature.certificates.add.qr.QrFormatException
import com.engboost.encryptedca.feature.certificates.add.qr.QrImageReader
import com.engboost.encryptedca.feature.certificates.add.qr.QrProfile
import com.engboost.encryptedca.feature.certificates.add.qr.QrProfileAssembler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Picked document URIs survive process death in [savedState]. A profile read from QR codes stays only
 * in memory and is wiped when it's replaced or the screen goes away. The password never leaves the
 * screen until import.
 */
internal class AddProfileViewModel(
    private val repository: CertificateProfileRepository,
    private val documents: DocumentReader,
    private val qrImages: QrImageReader,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(AddProfileState())
    val state: StateFlow<AddProfileState> = _state.asStateFlow()

    private val qrAssembler = QrProfileAssembler()
    private var qrProfile: QrProfile? = null

    init {
        savedState.get<Uri>(KEY_P12)?.let(::selectP12)
        savedState.get<Uri>(KEY_CA)?.let(::selectCa)
    }

    fun selectP12(uri: Uri) = select(KEY_P12, uri) { state, document -> state.copy(p12 = document) }

    fun selectCa(uri: Uri) = select(KEY_CA, uri) { state, document -> state.copy(ca = document) }

    fun startQrScan() {
        clearQr()
        _state.update { it.copy(qr = QrStatus.Collecting()) }
    }

    /** Closing the scanner before all codes are read drops what was collected. */
    fun closeQrScan() {
        if (_state.value.qr is QrStatus.Collecting) clearQr()
    }

    /** Codes from one camera frame; they may belong to other profiles or not be ours at all. */
    fun onQrCodes(texts: List<String>) {
        if (_state.value.qr !is QrStatus.Collecting) return
        if (addQrCodes(texts)) showQrProgress()
    }

    fun readQrImage(uri: Uri) {
        viewModelScope.launch {
            val texts = qrImages.read(uri)
            if (_state.value.qr !is QrStatus.Collecting) return@launch
            if (addQrCodes(texts)) {
                showQrProgress()
            } else {
                _state.update { it.copy(qr = collectingStatus(imageWithoutNewCodes = true)) }
            }
        }
    }

    fun useFilesInsteadOfQr() = clearQr()

    /** Takes ownership of [password] and wipes it, even when the import doesn't start. */
    fun importProfile(displayName: String, password: CharArray) {
        val current = _state.value
        if (!current.canImport) {
            password.wipe()
            return
        }
        val name = displayName.trim().ifEmpty { null }
        val qr = qrProfile.takeIf { current.qr == QrStatus.Ready }
        val p12Uri = current.p12?.uri
        val caUri = current.ca?.uri
        _state.update { it.copy(status = ImportStatus.Importing) }
        viewModelScope.launch {
            val status = runImport(password) {
                when {
                    qr != null -> importFromQr(name, qr, password)
                    p12Uri != null && caUri != null -> importFromFiles(name, p12Uri, caUri, password)
                    else -> error("Nothing to import")
                }
            }
            _state.update { it.copy(status = status) }
        }
    }

    override fun onCleared() {
        qrProfile?.wipe()
    }

    /**
     * Shows the new document at once and fills in its name when the provider answers. A late answer
     * for a document that has been replaced since is dropped.
     */
    private fun select(key: String, uri: Uri, show: (AddProfileState, PickedDocument) -> AddProfileState) {
        clearQr()
        savedState[key] = uri
        _state.update { show(it, PickedDocument(uri, name = null)) }
        viewModelScope.launch {
            val name = documents.displayName(uri)
            if (savedState.get<Uri>(key) == uri) _state.update { show(it, PickedDocument(uri, name)) }
        }
    }

    /** Returns whether any of [texts] was a new part of the profile. */
    private fun addQrCodes(texts: List<String>): Boolean = texts.map(qrAssembler::add).any { it }

    private fun showQrProgress() {
        if (!qrAssembler.complete) {
            _state.update { it.copy(qr = collectingStatus()) }
            return
        }
        val status = try {
            qrProfile = qrAssembler.assemble()
            QrStatus.Ready
        } catch (e: QrFormatException) {
            Log.w(TAG, "QR codes don't form a profile", e)
            QrStatus.Invalid
        }
        qrAssembler.reset()
        if (status == QrStatus.Ready) clearFiles()
        _state.update { it.copy(qr = status) }
    }

    private fun collectingStatus(imageWithoutNewCodes: Boolean = false) =
        QrStatus.Collecting(qrAssembler.received, qrAssembler.total, imageWithoutNewCodes)

    private fun clearQr() {
        qrAssembler.reset()
        qrProfile?.wipe()
        qrProfile = null
        _state.update { it.copy(qr = null) }
    }

    private fun clearFiles() {
        savedState.remove<Uri>(KEY_P12)
        savedState.remove<Uri>(KEY_CA)
        _state.update { it.copy(p12 = null, ca = null) }
    }

    private suspend fun importFromFiles(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): String {
        val p12 = documents.read(p12Uri)
        try {
            val ca = documents.read(caUri)
            return repository.importProfile(displayName, p12, password, ca)
        } finally {
            p12.fill(0)
        }
    }

    /** The repository wipes what it gets, so it gets a copy: after a wrong password the codes needn't be scanned again. */
    private suspend fun importFromQr(displayName: String?, qr: QrProfile, password: CharArray): String {
        val p12 = qr.p12.copyOf()
        try {
            return repository.importProfile(displayName, p12, password, qr.caCertificate.copyOf())
        } finally {
            p12.fill(0)
        }
    }

    private suspend fun runImport(password: CharArray, import: suspend () -> String): ImportStatus =
        try {
            ImportStatus.Imported(import())
        } catch (e: CancellationException) {
            throw e
        } catch (e: CertificateProfileException) {
            Log.w(TAG, "Import failed: ${e.error}", e)
            ImportStatus.Failed(e.error)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected import failure", e)
            ImportStatus.Failed(CertificateProfileError.STORAGE_FAILED)
        } finally {
            password.wipe()
        }

    private companion object {
        const val TAG = "AddProfile"
        const val KEY_P12 = "p12_uri"
        const val KEY_CA = "ca_uri"
    }
}

package com.engboost.encryptedca.feature.certificates.add

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.wipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A picked document; [name] is `null` when the provider doesn't report one. */
internal data class PickedDocument(val name: String?)

@Immutable
internal sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Importing : ImportStatus
    data class Imported(val profileId: String) : ImportStatus
    data class Failed(val error: CertificateProfileError) : ImportStatus
}

internal data class AddProfileState(
    val p12: PickedDocument? = null,
    val ca: PickedDocument? = null,
    val status: ImportStatus = ImportStatus.Idle,
) {
    val importing: Boolean get() = status == ImportStatus.Importing
    val canImport: Boolean get() = p12 != null && ca != null && !importing
}

internal class AddProfileViewModel(
    private val repository: CertificateProfileRepository,
    private val documents: DocumentReader,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(AddProfileState())
    val state: StateFlow<AddProfileState> = _state.asStateFlow()

    init {
        savedState.get<Uri>(KEY_P12)?.let(this::selectP12)
        savedState.get<Uri>(KEY_CA)?.let(this::selectCa)
    }

    fun selectP12(uri: Uri) = select(KEY_P12, uri) { state, document -> state.copy(p12 = document) }

    fun selectCa(uri: Uri) = select(KEY_CA, uri) { state, document -> state.copy(ca = document) }

    /** Takes ownership of [password] and wipes it, even when the import doesn't start. */
    fun importProfile(displayName: String, password: CharArray) {
        val p12 = savedState.get<Uri>(KEY_P12)
        val ca = savedState.get<Uri>(KEY_CA)
        if (p12 == null || ca == null || _state.value.importing) {
            password.wipe()
            return
        }
        _state.update { it.copy(status = ImportStatus.Importing) }
        viewModelScope.launch {
            val status = importDocuments(displayName.trim().ifEmpty { null }, p12, ca, password)
            _state.update { it.copy(status = status) }
        }
    }

    private fun select(key: String, uri: Uri, apply: (AddProfileState, PickedDocument) -> AddProfileState) {
        savedState[key] = uri
        viewModelScope.launch {
            val document = PickedDocument(documents.displayName(uri))
            _state.update { apply(it, document) }
        }
    }

    private suspend fun importDocuments(displayName: String?, p12Uri: Uri, caUri: Uri, password: CharArray): ImportStatus {
        var p12: ByteArray? = null
        return try {
            p12 = documents.read(p12Uri)
            val ca = documents.read(caUri)
            ImportStatus.Imported(repository.importProfile(displayName, p12, password, ca))
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
            p12?.fill(0)
        }
    }

    private companion object {
        const val TAG = "AddProfile"
        const val KEY_P12 = "p12_uri"
        const val KEY_CA = "ca_uri"
    }
}

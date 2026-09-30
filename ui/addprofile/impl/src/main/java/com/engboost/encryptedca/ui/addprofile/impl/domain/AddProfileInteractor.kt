// Adds a profile from files or from QR codes. The source is picked on the profile list and doesn't change.
// QR codes live only in the QrCollection and are cleared with clear().
package com.engboost.encryptedca.ui.addprofile.impl.domain

import android.util.Log
import com.engboost.encryptedca.core.certificates.api.wipe
import com.engboost.encryptedca.feature.certificates.api.CertificatesFeature
import com.engboost.encryptedca.feature.certificates.api.entity.ImportResult
import com.engboost.encryptedca.feature.certificates.api.entity.QrCollectResult
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.AddProfileState
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.ImportStatus
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.PickedDocument
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.QrStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class AddProfileInteractor(
    private val certificatesFeature: CertificatesFeature,
    source: ProfileSource,
) {
    private val qrCollection = certificatesFeature.newQrCollection()

    private val _state = MutableStateFlow(AddProfileState(source))
    val state: StateFlow<AddProfileState> = _state.asStateFlow()

    init {
        if (source != ProfileSource.FILES) startQrCollecting()
    }

    suspend fun pickP12(uri: String) =
        pickDocument(uri, AddProfileState::p12) { state, document -> state.copy(p12 = document) }

    suspend fun pickCa(uri: String) =
        pickDocument(uri, AddProfileState::ca) { state, document -> state.copy(ca = document) }

    fun startQrCollecting() {
        qrCollection.clear()
        _state.update { it.copy(qr = collecting()) }
    }

    fun addQrCodes(texts: List<String>) {
        if (_state.value.qr !is QrStatus.Collecting) return
        val status = qrCollection.add(texts).toStatus() ?: return
        _state.update { it.copy(qr = status) }
    }

    suspend fun readQrPhotos(uris: List<String>) {
        val texts = certificatesFeature.readQrCodes(uris)
        if (_state.value.qr !is QrStatus.Collecting) return
        val status = qrCollection.add(texts).toStatus() ?: collecting(photosWithoutNewCodes = true)
        _state.update { it.copy(qr = status) }
    }

    suspend fun importProfile(displayName: String, password: CharArray) {
        val current = _state.value
        if (!current.canImport) {
            password.wipe()
            return
        }
        val name = displayName.trim().ifEmpty { null }
        val p12 = current.p12
        val ca = current.ca
        _state.update { it.copy(importStatus = ImportStatus.Importing) }
        val result = if (p12 != null && ca != null) {
            certificatesFeature.importFromFiles(name, p12.uri, ca.uri, password)
        } else {
            qrCollection.importProfile(name, password)
        }
        if (result is ImportResult.Failed) Log.w(TAG, "Import failed: ${result.error}", result.cause)
        _state.update { it.copy(importStatus = result.toStatus()) }
    }

    fun clear() = qrCollection.clear()

    private suspend fun pickDocument(
        uri: String,
        current: (AddProfileState) -> PickedDocument?,
        show: (AddProfileState, PickedDocument) -> AddProfileState,
    ) {
        _state.update { show(it, PickedDocument(uri, name = null)) }
        val name = certificatesFeature.documentName(uri)
        _state.update { if (current(it)?.uri == uri) show(it, PickedDocument(uri, name)) else it }
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
    }
}

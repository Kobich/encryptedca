// Picked files are kept in SavedStateHandle, so they survive process death.
package com.engboost.encryptedca.ui.addprofile.impl.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource
import com.engboost.encryptedca.ui.addprofile.impl.domain.AddProfileInteractor
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.AddProfileState
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.ImportStatus
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.PickedDocument
import com.engboost.encryptedca.ui.addprofile.impl.domain.entity.QrStatus
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileViewState
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.DocumentViewState
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.QrProgressViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class AddProfileViewModel(
    private val interactor: AddProfileInteractor,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    val state: StateFlow<AddProfileViewState> = interactor.state
        .map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, interactor.state.value.toViewState())

    init {
        savedState.get<String>(KEY_P12)?.let(::pickP12)
        savedState.get<String>(KEY_CA)?.let(::pickCa)
    }

    fun pickP12(uri: String) {
        savedState[KEY_P12] = uri
        viewModelScope.launch { interactor.pickP12(uri) }
    }

    fun pickCa(uri: String) {
        savedState[KEY_CA] = uri
        viewModelScope.launch { interactor.pickCa(uri) }
    }

    fun addQrCodes(texts: List<String>) = interactor.addQrCodes(texts)

    fun readQrPhotos(uris: List<String>) {
        viewModelScope.launch { interactor.readQrPhotos(uris) }
    }

    fun collectQrAgain() = interactor.startQrCollecting()

    fun importProfile(displayName: String, password: CharArray) {
        viewModelScope.launch { interactor.importProfile(displayName, password) }
    }

    override fun onCleared() = interactor.clear()

    private companion object {
        const val KEY_P12 = "p12_uri"
        const val KEY_CA = "ca_uri"
    }
}

private fun AddProfileState.toViewState(): AddProfileViewState {
    val qr = qr
    return when {
        qr is QrStatus.Collecting && source == ProfileSource.QR_CAMERA -> AddProfileViewState.QrCamera(qr.toViewState())
        qr is QrStatus.Collecting && source == ProfileSource.QR_PHOTOS -> AddProfileViewState.QrPhotos(qr.toViewState())
        else -> AddProfileViewState.Form(
            source = source,
            p12 = p12?.toViewState(),
            ca = ca?.toViewState(),
            qrInvalid = qr == QrStatus.Invalid,
            importing = importing,
            canImport = canImport,
            importError = (importStatus as? ImportStatus.Failed)?.error,
            imported = importStatus is ImportStatus.Imported,
        )
    }
}

private fun QrStatus.Collecting.toViewState() = QrProgressViewState(received, total, photosWithoutNewCodes)

private fun PickedDocument.toViewState() = DocumentViewState(name)

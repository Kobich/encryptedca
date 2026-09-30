package com.engboost.encryptedca.ui.addprofile.impl.ui

import androidx.compose.runtime.Composable
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileCallbacks
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileViewState

@Composable
internal fun AddProfileScreenView(
    state: AddProfileViewState,
    callbacks: AddProfileCallbacks,
) {
    when (state) {
        is AddProfileViewState.QrCamera -> QrCameraView(progress = state.progress, callbacks = callbacks)
        is AddProfileViewState.QrPhotos -> QrPhotosView(progress = state.progress, callbacks = callbacks)
        is AddProfileViewState.Form -> AddProfileFormView(state = state, callbacks = callbacks)
    }
}

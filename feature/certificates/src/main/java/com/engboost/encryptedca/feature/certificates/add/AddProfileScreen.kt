package com.engboost.encryptedca.feature.certificates.add

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.engboost.encryptedca.feature.certificates.R
import com.engboost.encryptedca.feature.certificates.add.components.AddProfileForm
import com.engboost.encryptedca.feature.certificates.add.qr.QrCameraScreen
import com.engboost.encryptedca.feature.certificates.add.qr.QrPhotosScreen

@Composable
internal fun AddProfileScreen(
    state: AddProfileState,
    onAction: (AddProfileAction) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val close: () -> Unit = {
        if (state.importing) {
            Toast.makeText(context, R.string.import_wait_before_leaving, Toast.LENGTH_SHORT).show()
        } else {
            onClose()
        }
    }
    BackHandler(onBack = close)

    LaunchedEffect(state.status) {
        if (state.status is ImportStatus.Imported) onClose()
    }

    val qr = state.qr
    when {
        qr is QrStatus.Collecting && state.source == ProfileSource.QR_CAMERA -> QrCameraScreen(
            status = qr,
            onCodes = { onAction(AddProfileAction.QrCodesScanned(it)) },
            onClose = close,
        )
        qr is QrStatus.Collecting && state.source == ProfileSource.QR_PHOTOS -> QrPhotosScreen(
            status = qr,
            onPhotosPicked = { onAction(AddProfileAction.QrPhotosPicked(it)) },
            onClose = close,
        )
        else -> AddProfileForm(state = state, onAction = onAction, onBack = close)
    }
}

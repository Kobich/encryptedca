package com.engboost.encryptedca.ui.addprofile.impl.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.addprofile.impl.R
import com.engboost.encryptedca.ui.addprofile.impl.ui.components.QrCollectingProgress
import com.engboost.encryptedca.ui.addprofile.impl.ui.components.QrCollectingScaffold
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileCallbacks
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.QrProgressViewState

@Composable
internal fun QrPhotosView(
    progress: QrProgressViewState,
    callbacks: AddProfileCallbacks,
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) callbacks.onQrPhotosPicked(uris.map { it.toString() })
    }
    val pickPhotos = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    var pickerOpenedOnStart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!pickerOpenedOnStart) {
            pickerOpenedOnStart = true
            pickPhotos()
        }
    }

    QrCollectingScaffold(title = R.string.qr_photos_title, onClose = callbacks.onClose) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.qr_photos_hint), style = MaterialTheme.typography.bodyLarge)
            QrCollectingProgress(progress)
            Button(onClick = pickPhotos, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (progress.total == 0) R.string.qr_pick_photos else R.string.qr_pick_more_photos))
            }
        }
    }
}

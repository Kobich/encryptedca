package com.engboost.encryptedca.feature.certificates.ui.impl.add.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.QrStatus
import com.engboost.encryptedca.feature.certificates.ui.impl.R

@Composable
internal fun QrCameraScreen(
    status: QrStatus.Collecting,
    onCodes: (List<String>) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var cameraAllowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraAllowed = it }
    LaunchedEffect(Unit) {
        if (!cameraAllowed) permissionRequest.launch(Manifest.permission.CAMERA)
    }

    QrCollectingScaffold(title = R.string.qr_camera_title, onClose = onClose) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (cameraAllowed) {
                QrCameraPreview(onCodes = onCodes, modifier = Modifier.fillMaxSize())
            } else {
                CameraPermissionRequest(onAllow = { permissionRequest.launch(Manifest.permission.CAMERA) })
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (status.total == 0) Text(stringResource(R.string.qr_aim))
            QrCollectingProgress(status)
        }
    }
}

@Composable
private fun CameraPermissionRequest(onAllow: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(24.dp),
    ) {
        Text(stringResource(R.string.qr_camera_permission_needed), textAlign = TextAlign.Center)
        Button(onClick = onAllow) { Text(stringResource(R.string.qr_allow_camera)) }
    }
}

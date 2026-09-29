package com.engboost.encryptedca.feature.certificates.add.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.engboost.encryptedca.feature.certificates.R
import com.engboost.encryptedca.feature.certificates.add.QrStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QrScanScreen(
    status: QrStatus.Collecting,
    onCodes: (List<String>) -> Unit,
    onPickImage: () -> Unit,
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
    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.qr_close))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (cameraAllowed) {
                    QrCameraPreview(onCodes = onCodes, modifier = Modifier.fillMaxSize())
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(stringResource(R.string.qr_camera_permission_needed), textAlign = TextAlign.Center)
                        Button(onClick = { permissionRequest.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.qr_allow_camera))
                        }
                    }
                }
            }
            ScanProgress(status, onPickImage)
        }
    }
}

@Composable
private fun ScanProgress(status: QrStatus.Collecting, onPickImage: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (status.total == 0) {
            Text(stringResource(R.string.qr_aim))
        } else {
            Text(stringResource(R.string.qr_progress, status.received, status.total))
            LinearProgressIndicator(
                progress = { status.received / status.total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (status.imageWithoutNewCodes) {
            Text(stringResource(R.string.qr_image_without_new_codes), color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(onClick = onPickImage) { Text(stringResource(R.string.qr_from_image)) }
    }
}

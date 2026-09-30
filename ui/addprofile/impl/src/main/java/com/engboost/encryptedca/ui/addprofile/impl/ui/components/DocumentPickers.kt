package com.engboost.encryptedca.ui.addprofile.impl.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.addprofile.impl.R
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileCallbacks
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileViewState
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.DocumentViewState

private val P12_MIME_TYPES = arrayOf("*/*")
private val CA_MIME_TYPES = arrayOf("application/x-pem-file", "application/x-x509-ca-cert", "text/plain", "*/*")

@Composable
internal fun DocumentPickers(state: AddProfileViewState.Form, callbacks: AddProfileCallbacks) {
    val p12Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { callbacks.onP12Picked(it.toString()) }
    }
    val caPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { callbacks.onCaPicked(it.toString()) }
    }

    DocumentPicker(
        label = R.string.client_certificate_label,
        action = R.string.select_p12,
        document = state.p12,
        enabled = !state.importing,
        onPick = { p12Picker.launch(P12_MIME_TYPES) },
    )
    DocumentPicker(
        label = R.string.ca_certificate_label,
        action = R.string.select_ca,
        document = state.ca,
        enabled = !state.importing,
        onPick = { caPicker.launch(CA_MIME_TYPES) },
    )
}

@Composable
private fun DocumentPicker(
    @StringRes label: Int,
    @StringRes action: Int,
    document: DocumentViewState?,
    enabled: Boolean,
    onPick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        Text(
            text = when {
                document == null -> stringResource(R.string.file_not_selected)
                else -> document.name ?: stringResource(R.string.document_name_unknown)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onPick, enabled = enabled) {
            Text(stringResource(action))
        }
    }
}

package com.engboost.encryptedca.feature.certificates.ui.impl.add.components

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
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.AddProfileAction
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.AddProfileState
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.PickedDocument
import com.engboost.encryptedca.feature.certificates.ui.impl.R

private val P12_MIME_TYPES = arrayOf("*/*")
private val CA_MIME_TYPES = arrayOf("application/x-pem-file", "application/x-x509-ca-cert", "text/plain", "*/*")

@Composable
internal fun DocumentPickers(state: AddProfileState, onAction: (AddProfileAction) -> Unit) {
    val p12Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onAction(AddProfileAction.P12Picked(it)) }
    }
    val caPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onAction(AddProfileAction.CaPicked(it)) }
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
    document: PickedDocument?,
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

package com.engboost.encryptedca.feature.certificates.add

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.core.certificates.CertificateProfileError
import com.engboost.encryptedca.feature.certificates.R
import com.engboost.encryptedca.feature.certificates.messageRes

private val P12_MIME_TYPES = arrayOf("*/*")
private val CA_MIME_TYPES = arrayOf("application/x-pem-file", "application/x-x509-ca-cert", "text/plain", "*/*")

/** Keeps what the ViewModel must not see: the form fields, the password and the document pickers. */
@Composable
internal fun AddProfileScreen(
    state: AddProfileState,
    onP12Picked: (Uri) -> Unit,
    onCaPicked: (Uri) -> Unit,
    onImport: (displayName: String, password: CharArray) -> Unit,
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

    val p12Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onP12Picked)
    }
    val caPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onCaPicked)
    }

    var displayName by rememberSaveable { mutableStateOf("") }
    // Deliberately not saveable: the password must not end up in the saved instance state.
    var password by remember { mutableStateOf("") }

    AddProfileContent(
        state = state,
        displayName = displayName,
        onDisplayNameChange = { displayName = it },
        password = password,
        onPasswordChange = { password = it },
        onPickP12 = { p12Picker.launch(P12_MIME_TYPES) },
        onPickCa = { caPicker.launch(CA_MIME_TYPES) },
        onImport = {
            onImport(displayName, password.toCharArray())
            password = ""
        },
        onBack = close,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddProfileContent(
    state: AddProfileState,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onPickP12: () -> Unit,
    onPickCa: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_profiles),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.import_description), style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text(stringResource(R.string.display_name_hint)) },
                singleLine = true,
                enabled = !state.importing,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            DocumentPicker(
                label = R.string.client_certificate_label,
                action = R.string.select_p12,
                document = state.p12,
                enabled = !state.importing,
                onPick = onPickP12,
            )
            DocumentPicker(
                label = R.string.ca_certificate_label,
                action = R.string.select_ca,
                document = state.ca,
                enabled = !state.importing,
                onPick = onPickCa,
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.password_hint)) },
                singleLine = true,
                enabled = !state.importing,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onImport,
                enabled = state.canImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.import_profile))
            }

            ImportStatusMessage(state.status)
        }
    }
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

@Composable
private fun ImportStatusMessage(status: ImportStatus) {
    when (status) {
        ImportStatus.Importing -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.importing))
        }
        is ImportStatus.Failed -> Text(
            text = stringResource(status.error.messageRes()),
            color = MaterialTheme.colorScheme.error,
        )
        ImportStatus.Idle, is ImportStatus.Imported -> Unit
    }
}

@Preview(showBackground = true)
@Composable
private fun AddProfileContentPreview() {
    MaterialTheme {
        AddProfileContent(
            state = AddProfileState(
                p12 = PickedDocument("client.p12"),
                status = ImportStatus.Failed(CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT),
            ),
            displayName = "Lab router",
            onDisplayNameChange = {},
            password = "1234",
            onPasswordChange = {},
            onPickP12 = {},
            onPickCa = {},
            onImport = {},
            onBack = {},
        )
    }
}

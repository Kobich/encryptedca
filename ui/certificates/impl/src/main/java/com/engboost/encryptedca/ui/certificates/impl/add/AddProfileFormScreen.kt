// The add-profile form screen: name, certificates (files or the QR result), password and the import button.
// The password uses remember, not rememberSaveable, so it never ends up in the saved screen state.
package com.engboost.encryptedca.ui.certificates.impl.add

import android.net.Uri
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.feature.certificates.api.model.ProfileSource
import com.engboost.encryptedca.ui.certificates.impl.R
import com.engboost.encryptedca.ui.certificates.impl.add.components.DocumentPickers
import com.engboost.encryptedca.ui.certificates.impl.add.components.QrResult
import com.engboost.encryptedca.ui.certificates.impl.common.messageRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddProfileFormScreen(
    state: AddProfileState,
    onAction: (AddProfileAction) -> Unit,
    onBack: () -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_profiles))
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
            Text(stringResource(state.source.formDescription()), style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.display_name_hint)) },
                singleLine = true,
                enabled = !state.importing,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            when (state.source) {
                ProfileSource.FILES -> DocumentPickers(state, onAction)
                ProfileSource.QR_CAMERA, ProfileSource.QR_PHOTOS -> QrResult(
                    state = state,
                    onCollectAgain = { onAction(AddProfileAction.CollectQrAgain) },
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_hint)) },
                singleLine = true,
                enabled = !state.importing,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onAction(AddProfileAction.Import(displayName, password.toCharArray()))
                    password = ""
                },
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

@StringRes
private fun ProfileSource.formDescription(): Int = when (this) {
    ProfileSource.FILES -> R.string.import_files_description
    ProfileSource.QR_CAMERA, ProfileSource.QR_PHOTOS -> R.string.import_qr_description
}

@Preview(showBackground = true)
@Composable
private fun AddProfileFormScreenPreview() {
    MaterialTheme {
        AddProfileFormScreen(
            state = AddProfileState(
                source = ProfileSource.FILES,
                p12 = PickedDocument(Uri.EMPTY, "client.p12"),
                status = ImportStatus.Failed(CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

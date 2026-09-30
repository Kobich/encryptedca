package com.engboost.encryptedca.ui.profiles.impl.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.engboost.encryptedca.ui.profiles.impl.R
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ConfirmationViewState

@Composable
internal fun ConfirmationDialog(
    confirmation: ConfirmationViewState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val text = confirmation.dialogText()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(text.title)) },
        text = { Text(stringResource(text.message, confirmation.profile.label())) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(text.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private class DialogText(
    @StringRes val title: Int,
    @StringRes val message: Int,
    @StringRes val confirm: Int,
)

private fun ConfirmationViewState.dialogText() = when (this) {
    is ConfirmationViewState.Select -> DialogText(
        title = R.string.select_profile_title,
        message = R.string.select_profile_confirmation,
        confirm = R.string.select,
    )
    is ConfirmationViewState.Delete -> DialogText(
        title = R.string.delete_profile_title,
        message = R.string.delete_profile_confirmation,
        confirm = R.string.delete,
    )
}

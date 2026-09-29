package com.engboost.encryptedca.feature.certificates.list

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.certificates.R
import com.engboost.encryptedca.feature.certificates.messageRes
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileListScreen(
    state: ProfileListState,
    onAddProfile: () -> Unit,
    onAddProfileFromQr: () -> Unit,
    onBack: () -> Unit,
    onSelect: (ProfileItem) -> Unit,
    onDelete: (ProfileItem) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onRetryLoad: () -> Unit,
) {
    state.pendingAction?.let { ConfirmationDialog(it, onConfirm, onDismiss) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(onClick = onAddProfileFromQr) {
                    Icon(painterResource(R.drawable.ic_qr_code), contentDescription = stringResource(R.string.add_profile_qr))
                }
                FloatingActionButton(onClick = onAddProfile) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_profile))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ListHeader(state, onRetryLoad) }
            items(state.profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    active = profile.id == state.activeProfileId,
                    onSelect = { onSelect(profile) },
                    onDelete = { onDelete(profile) },
                )
            }
        }
    }
}

@Composable
private fun ListHeader(state: ProfileListState, onRetryLoad: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                if (state.activeProfileId == null) R.string.no_active_profile else R.string.active_profile_selected,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        val listLoad = state.listLoad
        if (listLoad is ListLoad.Failed) {
            Text(stringResource(listLoad.error.messageRes()), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetryLoad) { Text(stringResource(R.string.retry)) }
            return@Column
        }
        val error = state.changeError
        Text(
            text = stringResource(
                when {
                    listLoad == ListLoad.Loading || state.changing -> R.string.profiles_loading
                    error != null -> error.messageRes()
                    state.profiles.isEmpty() -> R.string.profiles_empty
                    else -> R.string.profiles_hint
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (error != null && !state.changing) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileItem,
    active: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val name = profile.label()
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !active, onClick = onSelect)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (active) {
                    Text(
                        text = stringResource(R.string.profile_active),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (profile.createdAt > 0) {
                    val date = remember(profile.createdAt) {
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(profile.createdAt))
                    }
                    Text(
                        text = stringResource(R.string.profile_added, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(end = 8.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_profile_accessibility, name),
                )
            }
        }
    }
}

private class DialogText(
    @StringRes val title: Int,
    @StringRes val message: Int,
    @StringRes val confirm: Int,
)

private fun PendingAction.dialogText() = when (this) {
    is PendingAction.Select -> DialogText(
        title = R.string.select_profile_title,
        message = R.string.select_profile_confirmation,
        confirm = R.string.select,
    )
    is PendingAction.Delete -> DialogText(
        title = R.string.delete_profile_title,
        message = R.string.delete_profile_confirmation,
        confirm = R.string.delete,
    )
}

@Composable
private fun ConfirmationDialog(action: PendingAction, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val text = action.dialogText()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(text.title)) },
        text = { Text(stringResource(text.message, action.profile.label())) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(text.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ProfileItem.label(): String =
    name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unnamed_profile)

@Preview(showBackground = true)
@Composable
private fun ProfileListScreenPreview() {
    MaterialTheme {
        ProfileListScreen(
            state = ProfileListState(
                profiles = listOf(
                    ProfileItem("a", "Lab router", createdAt = 1_758_700_000_000),
                    ProfileItem("b", null, createdAt = 0),
                ),
                activeProfileId = "a",
                listLoad = ListLoad.Loaded,
            ),
            onAddProfile = {},
            onAddProfileFromQr = {},
            onBack = {},
            onSelect = {},
            onDelete = {},
            onConfirm = {},
            onDismiss = {},
            onRetryLoad = {},
        )
    }
}

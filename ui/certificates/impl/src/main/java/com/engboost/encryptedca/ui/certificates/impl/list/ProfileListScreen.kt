package com.engboost.encryptedca.ui.certificates.impl.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.certificates.impl.R
import com.engboost.encryptedca.ui.certificates.impl.add.ProfileSource
import com.engboost.encryptedca.ui.certificates.impl.list.components.ConfirmationDialog
import com.engboost.encryptedca.ui.certificates.impl.list.components.ProfileCard
import com.engboost.encryptedca.ui.certificates.impl.list.components.ProfileListHeader
import com.engboost.encryptedca.ui.certificates.impl.list.components.ProfileSourceSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileListScreen(
    state: ProfileListState,
    onAction: (ProfileListAction) -> Unit,
    onAddProfile: (ProfileSource) -> Unit,
    onBack: () -> Unit,
) {
    state.pendingConfirmation?.let { confirmation ->
        ConfirmationDialog(
            confirmation = confirmation,
            onConfirm = { onAction(ProfileListAction.Confirm) },
            onDismiss = { onAction(ProfileListAction.Dismiss) },
        )
    }

    var choosingSource by rememberSaveable { mutableStateOf(false) }
    if (choosingSource) {
        ProfileSourceSheet(
            onPick = { source ->
                choosingSource = false
                onAddProfile(source)
            },
            onDismiss = { choosingSource = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { choosingSource = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_profile))
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
            item {
                ProfileListHeader(state, onRetryLoad = { onAction(ProfileListAction.RetryLoad) })
            }
            items(state.profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    active = profile.id == state.activeProfileId,
                    onSelect = { onAction(ProfileListAction.Select(profile)) },
                    onDelete = { onAction(ProfileListAction.Delete(profile)) },
                )
            }
        }
    }
}

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
            onAction = {},
            onAddProfile = {},
            onBack = {},
        )
    }
}

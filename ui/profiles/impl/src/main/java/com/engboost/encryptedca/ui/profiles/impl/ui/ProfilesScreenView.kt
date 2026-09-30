package com.engboost.encryptedca.ui.profiles.impl.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.profiles.impl.R
import com.engboost.encryptedca.ui.profiles.impl.ui.components.ConfirmationDialog
import com.engboost.encryptedca.ui.profiles.impl.ui.components.ProfileCard
import com.engboost.encryptedca.ui.profiles.impl.ui.components.ProfileSourceSheet
import com.engboost.encryptedca.ui.profiles.impl.ui.components.ProfilesHeader
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfileViewState
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesCallbacks
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesStatus
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfilesScreenView(
    state: ProfilesViewState,
    callbacks: ProfilesCallbacks,
) {
    state.confirmation?.let { confirmation ->
        ConfirmationDialog(
            confirmation = confirmation,
            onConfirm = callbacks.onConfirm,
            onDismiss = callbacks.onDismissConfirmation,
        )
    }

    if (state.sourceSheetVisible) {
        ProfileSourceSheet(onPick = callbacks.onSourcePicked, onDismiss = callbacks.onDismissSourceSheet)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                navigationIcon = {
                    IconButton(onClick = callbacks.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = callbacks.onAddProfile) {
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
                ProfilesHeader(state, onRetryLoad = callbacks.onRetryLoad)
            }
            items(state.profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    active = profile.id == state.activeProfileId,
                    onSelect = { callbacks.onSelect(profile) },
                    onDelete = { callbacks.onDelete(profile) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilesScreenViewPreview() {
    MaterialTheme {
        ProfilesScreenView(
            state = ProfilesViewState(
                profiles = listOf(
                    ProfileViewState("a", "Lab router", createdAt = 1_758_700_000_000),
                    ProfileViewState("b", null, createdAt = 0),
                ),
                activeProfileId = "a",
                status = ProfilesStatus.Ready,
            ),
            callbacks = ProfilesCallbacks(
                onBack = {},
                onRetryLoad = {},
                onSelect = {},
                onDelete = {},
                onConfirm = {},
                onDismissConfirmation = {},
                onAddProfile = {},
                onSourcePicked = {},
                onDismissSourceSheet = {},
            ),
        )
    }
}

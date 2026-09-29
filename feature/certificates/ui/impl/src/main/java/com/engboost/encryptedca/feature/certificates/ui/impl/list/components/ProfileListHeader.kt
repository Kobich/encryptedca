package com.engboost.encryptedca.feature.certificates.ui.impl.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.certificates.impl.presentation.list.ListLoad
import com.engboost.encryptedca.feature.certificates.impl.presentation.list.ProfileListState
import com.engboost.encryptedca.feature.certificates.ui.impl.R
import com.engboost.encryptedca.feature.certificates.ui.impl.common.messageRes

@Composable
internal fun ProfileListHeader(state: ProfileListState, onRetryLoad: () -> Unit) {
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

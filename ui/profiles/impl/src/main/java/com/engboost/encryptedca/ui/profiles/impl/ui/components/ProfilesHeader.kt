package com.engboost.encryptedca.ui.profiles.impl.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.profiles.impl.R
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesStatus
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesViewState
import com.engboost.encryptedca.ui.profiles.impl.ui.messageRes

@Composable
internal fun ProfilesHeader(state: ProfilesViewState, onRetryLoad: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                if (state.activeProfileId == null) R.string.no_active_profile else R.string.active_profile_selected,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        when (val status = state.status) {
            is ProfilesStatus.LoadFailed -> {
                Text(stringResource(status.error.messageRes()), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetryLoad) { Text(stringResource(R.string.retry)) }
            }
            is ProfilesStatus.ChangeFailed -> Text(
                text = stringResource(status.error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            ProfilesStatus.Loading -> Hint(R.string.profiles_loading)
            ProfilesStatus.Empty -> Hint(R.string.profiles_empty)
            ProfilesStatus.Ready -> Hint(R.string.profiles_hint)
        }
    }
}

@Composable
private fun Hint(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

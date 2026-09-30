package com.engboost.encryptedca.ui.profiles.impl.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.profiles.impl.R
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfileViewState
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ProfileCard(
    profile: ProfileViewState,
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

@Composable
internal fun ProfileViewState.label(): String =
    name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unnamed_profile)

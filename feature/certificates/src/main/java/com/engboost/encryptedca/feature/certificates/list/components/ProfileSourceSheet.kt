package com.engboost.encryptedca.feature.certificates.list.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.certificates.R
import com.engboost.encryptedca.feature.certificates.add.ProfileSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileSourceSheet(onPick: (ProfileSource) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.add_profile),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        ProfileSource.entries.forEach { source ->
            val option = source.option()
            ListItem(
                headlineContent = { Text(stringResource(option.title)) },
                supportingContent = { Text(stringResource(option.description)) },
                leadingContent = { Icon(painterResource(option.icon), contentDescription = null) },
                modifier = Modifier.clickable { onPick(source) },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

private class SourceOption(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
)

private fun ProfileSource.option() = when (this) {
    ProfileSource.FILES -> SourceOption(
        icon = R.drawable.ic_file,
        title = R.string.source_files,
        description = R.string.source_files_description,
    )
    ProfileSource.QR_CAMERA -> SourceOption(
        icon = R.drawable.ic_qr_code,
        title = R.string.source_qr_camera,
        description = R.string.source_qr_camera_description,
    )
    ProfileSource.QR_PHOTOS -> SourceOption(
        icon = R.drawable.ic_photo,
        title = R.string.source_qr_photos,
        description = R.string.source_qr_photos_description,
    )
}

package com.engboost.encryptedca.feature.certificates.ui.impl.add.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.certificates.impl.domain.model.ProfileSource
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.AddProfileState
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.QrStatus
import com.engboost.encryptedca.feature.certificates.ui.impl.R

@Composable
internal fun QrResult(state: AddProfileState, onCollectAgain: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.qr_section_label), style = MaterialTheme.typography.titleSmall)
        if (state.qr == QrStatus.Invalid) {
            Text(stringResource(R.string.qr_invalid), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onCollectAgain, enabled = !state.importing) {
                Text(
                    stringResource(
                        if (state.source == ProfileSource.QR_PHOTOS) R.string.qr_pick_photos_again else R.string.qr_scan_again,
                    ),
                )
            }
        } else {
            Text(stringResource(R.string.qr_ready), color = MaterialTheme.colorScheme.primary)
        }
    }
}

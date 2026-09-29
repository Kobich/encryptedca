package com.engboost.encryptedca.feature.certificates.ui.impl.add.qr

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.certificates.impl.presentation.add.QrStatus
import com.engboost.encryptedca.feature.certificates.ui.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QrCollectingScaffold(
    @StringRes title: Int,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.qr_close))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding), content = content)
    }
}

@Composable
internal fun QrCollectingProgress(status: QrStatus.Collecting) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (status.total > 0) {
            Text(stringResource(R.string.qr_progress, status.received, status.total))
            LinearProgressIndicator(
                progress = { status.received / status.total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (status.photosWithoutNewCodes) {
            Text(stringResource(R.string.qr_photos_without_new_codes), color = MaterialTheme.colorScheme.error)
        }
    }
}

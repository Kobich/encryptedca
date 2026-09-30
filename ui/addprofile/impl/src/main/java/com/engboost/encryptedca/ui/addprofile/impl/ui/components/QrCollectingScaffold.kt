package com.engboost.encryptedca.ui.addprofile.impl.ui.components

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
import com.engboost.encryptedca.ui.addprofile.impl.R
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.QrProgressViewState

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
internal fun QrCollectingProgress(progress: QrProgressViewState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (progress.total > 0) {
            Text(stringResource(R.string.qr_progress, progress.received, progress.total))
            LinearProgressIndicator(
                progress = { progress.received / progress.total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (progress.photosWithoutNewCodes) {
            Text(stringResource(R.string.qr_photos_without_new_codes), color = MaterialTheme.colorScheme.error)
        }
    }
}

package com.engboost.encryptedca.feature.scanner.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.scanner.R
import com.engboost.encryptedca.feature.scanner.ScannerState

@Composable
internal fun ScanStatus(state: ScannerState) {
    val status = when {
        state.problem != null -> return
        state.scanning -> R.string.scanner_scanning
        state.devices.isEmpty() -> R.string.scanner_nothing_found
        else -> R.string.scanner_finished
    }
    Text(
        text = stringResource(status),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
}

package com.engboost.encryptedca.feature.scanner.ui.impl.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.scanner.impl.domain.model.ScanProblem
import com.engboost.encryptedca.feature.scanner.impl.presentation.ScannerState
import com.engboost.encryptedca.feature.scanner.ui.impl.R

@Composable
internal fun ScanHeader(state: ScannerState, onOpenCertificates: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.scanner_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        state.localIp?.let {
            Text(
                text = stringResource(R.string.scanner_local_ip, it),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        val problem = when (state.problem) {
            ScanProblem.NO_WIFI -> R.string.scanner_no_wifi
            ScanProblem.NO_PROFILE -> R.string.scanner_no_profile
            ScanProblem.PROFILE_UNAVAILABLE -> R.string.scanner_profile_unavailable
            ScanProblem.SCAN_FAILED -> R.string.scanner_failed
            null -> null
        }
        problem?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (state.problem == ScanProblem.NO_PROFILE || state.problem == ScanProblem.PROFILE_UNAVAILABLE) {
            OutlinedButton(onClick = onOpenCertificates) { Text(stringResource(R.string.select_certificate)) }
        }
    }
}

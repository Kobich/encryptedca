package com.engboost.encryptedca.feature.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val ConnectableBlue = Color(0xFF1E88E5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerScreen(
    state: ScannerState,
    onRescan: () -> Unit,
    onOpenCertificates: () -> Unit,
    onOpenDevice: (ip: String, serverFingerprint: String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scanner_title)) },
                actions = {
                    IconButton(onClick = onOpenCertificates) {
                        Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.open_certificates))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onRescan, shape = CircleShape) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.rescan))
            }
        },
        bottomBar = { ScanStatus(state) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.scanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ScanHeader(state, onOpenCertificates) }
                if (state.devices.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            state.devices.forEachIndexed { index, device ->
                                if (index > 0) HorizontalDivider()
                                DeviceRow(
                                    device = device,
                                    onOpen = { device.serverFingerprint?.let { onOpenDevice(device.ip, it) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanHeader(state: ScannerState, onOpenCertificates: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            null -> null
        }
        problem?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (state.problem == ScanProblem.NO_PROFILE || state.problem == ScanProblem.PROFILE_UNAVAILABLE) {
            OutlinedButton(onClick = onOpenCertificates) { Text(stringResource(R.string.select_certificate)) }
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceItem, onOpen: () -> Unit) {
    Text(
        text = stringResource(R.string.device_ip, device.ip),
        style = MaterialTheme.typography.titleMedium,
        color = if (device.connectable) ConnectableBlue else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (device.connectable) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = device.connectable, onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

@Composable
private fun ScanStatus(state: ScannerState) {
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

@Preview(showBackground = true)
@Composable
private fun ScannerScreenPreview() {
    MaterialTheme {
        ScannerScreen(
            state = ScannerState(
                localIp = "192.168.1.152",
                scanning = false,
                devices = listOf(
                    DeviceItem("192.168.1.1", serverFingerprint = null),
                    DeviceItem("192.168.1.193", serverFingerprint = null),
                    DeviceItem("192.168.1.242", serverFingerprint = "ab12"),
                ),
            ),
            onRescan = {},
            onOpenCertificates = {},
            onOpenDevice = { _, _ -> },
        )
    }
}

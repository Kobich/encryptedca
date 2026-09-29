package com.engboost.encryptedca.feature.scanner

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.feature.scanner.components.DeviceRow
import com.engboost.encryptedca.feature.scanner.components.ScanHeader
import com.engboost.encryptedca.feature.scanner.components.ScanStatus

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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
        ) {
            item { ScanHeader(state, onOpenCertificates, modifier = Modifier.padding(bottom = 16.dp)) }
            itemsIndexed(state.devices, key = { _, device -> device.ip }) { index, device ->
                DeviceRow(
                    device = device,
                    first = index == 0,
                    last = index == state.devices.lastIndex,
                    onOpen = { device.serverFingerprint?.let { onOpenDevice(device.ip, it) } },
                )
            }
        }
    }
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

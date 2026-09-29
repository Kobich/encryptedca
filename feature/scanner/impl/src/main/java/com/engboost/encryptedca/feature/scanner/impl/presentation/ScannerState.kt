package com.engboost.encryptedca.feature.scanner.impl.presentation

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.feature.scanner.impl.domain.model.ScanProblem

data class DeviceItem(val ip: String, val serverFingerprint: String?) {
    val connectable: Boolean get() = serverFingerprint != null
}

@Immutable
data class ScannerState(
    val localIp: String? = null,
    val scanning: Boolean = true,
    val devices: List<DeviceItem> = emptyList(),
    val problem: ScanProblem? = null,
)

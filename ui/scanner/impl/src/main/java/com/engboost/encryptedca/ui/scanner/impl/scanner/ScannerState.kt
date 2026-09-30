package com.engboost.encryptedca.ui.scanner.impl.scanner

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.feature.scanner.api.entity.ScanProblem

internal data class DeviceItem(val ip: String, val serverFingerprint: String?) {
    val connectable: Boolean get() = serverFingerprint != null
}

@Immutable
internal data class ScannerState(
    val localIp: String? = null,
    val scanning: Boolean = true,
    val devices: List<DeviceItem> = emptyList(),
    val problem: ScanProblem? = null,
)

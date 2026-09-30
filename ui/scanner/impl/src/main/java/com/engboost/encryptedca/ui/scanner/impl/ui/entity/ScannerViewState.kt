package com.engboost.encryptedca.ui.scanner.impl.ui.entity

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.feature.scanner.api.entity.ScanProblem

@Immutable
internal data class ScannerViewState(
    val localIp: String? = null,
    val scanning: Boolean = true,
    val devices: List<DeviceViewState> = emptyList(),
    val problem: ScanProblem? = null,
)

internal data class DeviceViewState(val ip: String, val serverFingerprint: String?) {
    val connectable: Boolean get() = serverFingerprint != null
}

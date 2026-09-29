package com.engboost.encryptedca.feature.scanner

import androidx.compose.runtime.Immutable

internal enum class ScanProblem { NO_WIFI, NO_PROFILE, PROFILE_UNAVAILABLE, SCAN_FAILED }

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

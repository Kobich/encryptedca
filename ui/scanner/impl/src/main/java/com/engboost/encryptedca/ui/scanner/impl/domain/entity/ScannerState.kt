package com.engboost.encryptedca.ui.scanner.impl.domain.entity

import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import com.engboost.encryptedca.feature.scanner.api.entity.ScanProblem

internal data class ScannerState(
    val localIp: String?,
    val scanning: Boolean = true,
    val devices: List<FoundDevice> = emptyList(),
    val problem: ScanProblem? = null,
)

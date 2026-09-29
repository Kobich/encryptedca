package com.engboost.encryptedca.feature.scanner.impl.domain.model

import com.engboost.encryptedca.core.network.api.scan.FoundDevice

internal sealed interface ScanUpdate {
    data class NotStarted(val problem: ScanProblem) : ScanUpdate
    data object Started : ScanUpdate
    data class Found(val devices: List<FoundDevice>) : ScanUpdate
    data object Finished : ScanUpdate
    data class Failed(val cause: Throwable) : ScanUpdate
}

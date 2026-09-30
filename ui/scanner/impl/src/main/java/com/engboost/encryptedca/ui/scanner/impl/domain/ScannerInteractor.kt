// Scans the current Wi-Fi network with the active profile.
// A new network, another profile or rescan() cancels the running scan and starts a new one.
package com.engboost.encryptedca.ui.scanner.impl.domain

import android.util.Log
import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import com.engboost.encryptedca.feature.scanner.api.ScannerFeature
import com.engboost.encryptedca.feature.scanner.api.entity.ScanProblem
import com.engboost.encryptedca.feature.scanner.api.entity.ScanUpdate
import com.engboost.encryptedca.ui.scanner.impl.domain.entity.ScannerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.update

internal class ScannerInteractor(
    private val scannerFeature: ScannerFeature,
) {
    private val rescans = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<ScannerState> = combine(scannerFeature.networkToScan(), rescans) { wifi, _ -> wifi }
        .flatMapLatest(::scan)

    fun rescan() = rescans.update { it + 1 }

    private fun scan(wifi: LocalNetwork?): Flow<ScannerState> =
        scannerFeature.scan(wifi)
            .onEach { if (it is ScanUpdate.Failed) Log.w(TAG, "Scan failed", it.cause) }
            .runningFold(ScannerState(localIp = wifi?.address?.hostAddress)) { state, update -> state.after(update) }

    private fun ScannerState.after(update: ScanUpdate): ScannerState = when (update) {
        is ScanUpdate.NotStarted -> ScannerState(localIp, scanning = false, problem = update.problem)
        ScanUpdate.Started -> ScannerState(localIp)
        is ScanUpdate.Found -> copy(devices = update.devices)
        ScanUpdate.Finished -> copy(scanning = false)
        is ScanUpdate.Failed -> copy(scanning = false, problem = ScanProblem.SCAN_FAILED)
    }

    private companion object {
        const val TAG = "Scanner"
    }
}

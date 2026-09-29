// Scans the current Wi-Fi network with the active profile.
// A new network, another profile or the rescan button cancels the running scan and starts a new one.
package com.engboost.encryptedca.ui.scanner.impl.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.network.api.scan.FoundDevice
import com.engboost.encryptedca.feature.scanner.api.ScannerInteractor
import com.engboost.encryptedca.feature.scanner.api.model.ScanProblem
import com.engboost.encryptedca.feature.scanner.api.model.ScanUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ScannerViewModel(
    private val interactor: ScannerInteractor,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val rescans = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(interactor.networkToScan(), rescans) { wifi, _ -> wifi }
                .collectLatest { wifi ->
                    val localIp = wifi?.address?.hostAddress
                    interactor.scan(wifi).collect { update ->
                        if (update is ScanUpdate.Failed) Log.w(TAG, "Scan failed", update.cause)
                        _state.update { it.after(update, localIp) }
                    }
                }
        }
    }

    fun rescan() = rescans.update { it + 1 }

    private fun ScannerState.after(update: ScanUpdate, localIp: String?): ScannerState = when (update) {
        is ScanUpdate.NotStarted -> ScannerState(localIp = localIp, scanning = false, problem = update.problem)
        ScanUpdate.Started -> ScannerState(localIp = localIp)
        is ScanUpdate.Found -> copy(devices = update.devices.map(FoundDevice::toItem))
        ScanUpdate.Finished -> copy(scanning = false)
        is ScanUpdate.Failed -> copy(scanning = false, problem = ScanProblem.SCAN_FAILED)
    }

    private companion object {
        const val TAG = "Scanner"
    }
}

private fun FoundDevice.toItem() = DeviceItem(ip = ip, serverFingerprint = serverFingerprint)

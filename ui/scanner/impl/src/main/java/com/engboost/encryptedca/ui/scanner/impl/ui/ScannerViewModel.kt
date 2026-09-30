package com.engboost.encryptedca.ui.scanner.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import com.engboost.encryptedca.ui.scanner.impl.domain.ScannerInteractor
import com.engboost.encryptedca.ui.scanner.impl.domain.entity.ScannerState
import com.engboost.encryptedca.ui.scanner.impl.ui.entity.DeviceViewState
import com.engboost.encryptedca.ui.scanner.impl.ui.entity.ScannerViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class ScannerViewModel(
    private val interactor: ScannerInteractor,
) : ViewModel() {

    val state: StateFlow<ScannerViewState> = interactor.state
        .map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ScannerViewState())

    fun rescan() = interactor.rescan()
}

private fun ScannerState.toViewState() = ScannerViewState(
    localIp = localIp,
    scanning = scanning,
    devices = devices.map(FoundDevice::toViewState),
    problem = problem,
)

private fun FoundDevice.toViewState() = DeviceViewState(ip = ip, serverFingerprint = serverFingerprint)

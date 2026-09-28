package com.engboost.encryptedca.feature.scanner

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.CertificateProfileException
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.network.DeviceScanner
import com.engboost.encryptedca.core.network.FoundDevice
import com.engboost.encryptedca.core.network.LocalNetwork
import com.engboost.encryptedca.core.network.TlsSetupException
import com.engboost.encryptedca.core.network.createSslContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ScanProblem { NO_WIFI, NO_PROFILE, PROFILE_UNAVAILABLE }

/** [serverFingerprint] is set when the device passed the mTLS check, so its web panel can be opened. */
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

internal class ScannerViewModel(
    private val repository: CertificateProfileRepository,
    private val scanner: DeviceScanner,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val rescans = MutableStateFlow(0)

    init {
        // A new Wi-Fi network, another selected profile or a manual rescan restarts the scan.
        val activeProfile = repository.index.map { it?.activeProfileId }.distinctUntilChanged()
        viewModelScope.launch {
            combine(scanner.wifi(), activeProfile, rescans) { wifi, _, _ -> wifi }.collectLatest { wifi -> scan(wifi) }
        }
    }

    fun rescan() = rescans.update { it + 1 }

    private suspend fun scan(wifi: LocalNetwork?) {
        if (wifi == null) return showProblem(null, ScanProblem.NO_WIFI)
        val sslContext = try {
            repository.activeCredentials()?.createSslContext()
        } catch (e: CertificateProfileException) {
            return showProblem(wifi, ScanProblem.PROFILE_UNAVAILABLE)
        } catch (e: TlsSetupException) {
            return showProblem(wifi, ScanProblem.PROFILE_UNAVAILABLE)
        } ?: return showProblem(wifi, ScanProblem.NO_PROFILE)

        _state.value = ScannerState(localIp = wifi.address.hostAddress)
        val found = mutableListOf<FoundDevice>()
        scanner.scan(wifi, sslContext).collect { device ->
            found += device
            found.sort()
            _state.update { it.copy(devices = found.map(FoundDevice::toItem)) }
        }
        _state.update { it.copy(scanning = false) }
    }

    private fun showProblem(wifi: LocalNetwork?, problem: ScanProblem) {
        _state.value = ScannerState(localIp = wifi?.address?.hostAddress, scanning = false, problem = problem)
    }
}

private fun FoundDevice.toItem() = DeviceItem(ip = ip, serverFingerprint = serverFingerprint)

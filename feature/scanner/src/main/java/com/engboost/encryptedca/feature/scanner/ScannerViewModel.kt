// Сканирует текущую Wi-Fi сеть с активным профилем.
// Смена сети, смена профиля или кнопка «пересканировать» отменяют текущий скан и запускают новый.
// Ошибка завершает только текущую попытку, следующие сканы работают как обычно.
package com.engboost.encryptedca.feature.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.network.scan.DeviceScanner
import com.engboost.encryptedca.core.network.scan.FoundDevice
import com.engboost.encryptedca.core.network.tls.TlsSetupException
import com.engboost.encryptedca.core.network.tls.createSslContext
import com.engboost.encryptedca.core.network.wifi.LocalNetwork
import com.engboost.encryptedca.core.network.wifi.WifiMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.net.ssl.SSLContext

internal class ScannerViewModel(
    private val repository: CertificateProfileRepository,
    private val wifiMonitor: WifiMonitor,
    private val scanner: DeviceScanner,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val rescans = MutableStateFlow(0)

    init {
        val activeProfile = repository.index.map { it?.activeProfileId }.distinctUntilChanged()
        viewModelScope.launch {
            combine(wifiMonitor.observeNetwork(), activeProfile, rescans) { wifi, _, _ -> wifi }
                .collectLatest { wifi -> runScan(wifi) }
        }
    }

    fun rescan() = rescans.update { it + 1 }

    private suspend fun runScan(wifi: LocalNetwork?) {
        if (wifi == null) return showProblem(null, ScanProblem.NO_WIFI)
        val sslContext = try {
            createSslContext() ?: return showProblem(wifi, ScanProblem.NO_PROFILE)
        } catch (e: CertificateProfileException) {
            return showProblem(wifi, ScanProblem.PROFILE_UNAVAILABLE)
        } catch (e: TlsSetupException) {
            return showProblem(wifi, ScanProblem.PROFILE_UNAVAILABLE)
        }

        _state.value = ScannerState(localIp = wifi.address.hostAddress)
        val found = mutableListOf<FoundDevice>()
        try {
            scanner.scan(wifi, sslContext).collect { device ->
                found += device
                found.sort()
                _state.update { it.copy(devices = found.map(FoundDevice::toItem)) }
            }
            _state.update { it.copy(scanning = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Scan failed", e)
            _state.update { it.copy(scanning = false, problem = ScanProblem.SCAN_FAILED) }
        }
    }

    private suspend fun createSslContext(): SSLContext? = repository.loadActiveCredentials()?.createSslContext()

    private fun showProblem(wifi: LocalNetwork?, problem: ScanProblem) {
        _state.value = ScannerState(localIp = wifi?.address?.hostAddress, scanning = false, problem = problem)
    }

    private companion object {
        const val TAG = "Scanner"
    }
}

private fun FoundDevice.toItem() = DeviceItem(ip = ip, serverFingerprint = serverFingerprint)

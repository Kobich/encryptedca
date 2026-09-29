package com.engboost.encryptedca.feature.scanner.impl.interactor

import com.engboost.encryptedca.core.certificates.api.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileException
import com.engboost.encryptedca.core.network.api.scan.DeviceScanner
import com.engboost.encryptedca.core.network.api.scan.FoundDevice
import com.engboost.encryptedca.core.network.api.tls.SslContextFactory
import com.engboost.encryptedca.core.network.api.tls.TlsSetupException
import com.engboost.encryptedca.core.network.api.wifi.LocalNetwork
import com.engboost.encryptedca.core.network.api.wifi.WifiMonitor
import com.engboost.encryptedca.feature.scanner.api.ScannerInteractor
import com.engboost.encryptedca.feature.scanner.api.model.ScanProblem
import com.engboost.encryptedca.feature.scanner.api.model.ScanUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.runningFold
import javax.net.ssl.SSLContext

internal class DefaultScannerInteractor(
    private val repository: CertificateProfileRepository,
    private val wifiMonitor: WifiMonitor,
    private val scanner: DeviceScanner,
    private val sslContexts: SslContextFactory,
) : ScannerInteractor {

    override fun networkToScan(): Flow<LocalNetwork?> {
        val activeProfile = repository.index.map { it?.activeProfileId }.distinctUntilChanged()
        return combine(wifiMonitor.observeNetwork(), activeProfile) { wifi, _ -> wifi }
    }

    override fun scan(wifi: LocalNetwork?): Flow<ScanUpdate> = flow {
        if (wifi == null) return@flow emit(ScanUpdate.NotStarted(ScanProblem.NO_WIFI))
        val sslContext = try {
            createSslContext() ?: return@flow emit(ScanUpdate.NotStarted(ScanProblem.NO_PROFILE))
        } catch (e: CertificateProfileException) {
            return@flow emit(ScanUpdate.NotStarted(ScanProblem.PROFILE_UNAVAILABLE))
        } catch (e: TlsSetupException) {
            return@flow emit(ScanUpdate.NotStarted(ScanProblem.PROFILE_UNAVAILABLE))
        }

        emit(ScanUpdate.Started)
        emitAll(
            scanner.scan(wifi, sslContext)
                .runningFold(emptyList<FoundDevice>()) { found, device -> (found + device).sorted() }
                .drop(1)
                .map<List<FoundDevice>, ScanUpdate> { ScanUpdate.Found(it) }
                .onCompletion { cause -> if (cause == null) emit(ScanUpdate.Finished) }
                .catch { e -> emit(ScanUpdate.Failed(e)) },
        )
    }

    private suspend fun createSslContext(): SSLContext? =
        repository.loadActiveCredentials()?.let(sslContexts::create)
}

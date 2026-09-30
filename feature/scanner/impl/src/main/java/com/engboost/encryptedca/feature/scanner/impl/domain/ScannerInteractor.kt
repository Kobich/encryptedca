package com.engboost.encryptedca.feature.scanner.impl.domain

import com.engboost.encryptedca.core.certificates.api.ProfileStorageFeature
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileException
import com.engboost.encryptedca.core.network.api.NetworkFeature
import com.engboost.encryptedca.core.network.api.entity.FoundDevice
import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import com.engboost.encryptedca.core.network.api.entity.TlsSetupException
import com.engboost.encryptedca.feature.scanner.api.entity.ScanProblem
import com.engboost.encryptedca.feature.scanner.api.entity.ScanUpdate
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

internal class ScannerInteractor(
    private val profileStorage: ProfileStorageFeature,
    private val network: NetworkFeature,
) {

    fun networkToScan(): Flow<LocalNetwork?> {
        val activeProfile = profileStorage.index.map { it?.activeProfileId }.distinctUntilChanged()
        return combine(network.observeWifi(), activeProfile) { wifi, _ -> wifi }
    }

    fun scan(wifi: LocalNetwork?): Flow<ScanUpdate> = flow {
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
            network.scan(wifi, sslContext)
                .runningFold(emptyList<FoundDevice>()) { found, device -> (found + device).sorted() }
                .drop(1)
                .map<List<FoundDevice>, ScanUpdate> { ScanUpdate.Found(it) }
                .onCompletion { cause -> if (cause == null) emit(ScanUpdate.Finished) }
                .catch { e -> emit(ScanUpdate.Failed(e)) },
        )
    }

    private suspend fun createSslContext(): SSLContext? =
        profileStorage.loadActiveCredentials()?.let(network::createSslContext)
}

package com.engboost.encryptedca.feature.scanner.impl

import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import com.engboost.encryptedca.feature.scanner.api.ScannerFeature
import com.engboost.encryptedca.feature.scanner.api.entity.ScanUpdate
import com.engboost.encryptedca.feature.scanner.impl.domain.ScannerInteractor
import kotlinx.coroutines.flow.Flow

internal class ScannerFeatureImpl(
    private val interactor: ScannerInteractor,
) : ScannerFeature {
    override fun networkToScan(): Flow<LocalNetwork?> = interactor.networkToScan()

    override fun scan(wifi: LocalNetwork?): Flow<ScanUpdate> = interactor.scan(wifi)
}

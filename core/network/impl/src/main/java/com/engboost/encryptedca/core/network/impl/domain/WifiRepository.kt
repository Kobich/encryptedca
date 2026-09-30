package com.engboost.encryptedca.core.network.impl.domain

import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import kotlinx.coroutines.flow.Flow

internal interface WifiRepository {
    fun observeNetwork(): Flow<LocalNetwork?>
}

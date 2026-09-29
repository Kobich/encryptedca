package com.engboost.encryptedca.core.network.api.wifi

import kotlinx.coroutines.flow.Flow

interface WifiMonitor {
    fun observeNetwork(): Flow<LocalNetwork?>
}

// Scanning the current Wi-Fi network for devices that pass the mTLS check.
// networkToScan() emits the Wi-Fi network again whenever the network or the active profile changes.
// scan() is one scan of that network with the active profile: NotStarted when there is no Wi-Fi, no profile
// or the profile can't be loaded; otherwise Started, the sorted devices found so far after each new one,
// then Finished, or Failed if the scan breaks. Devices found before a failure stay in the last Found.
package com.engboost.encryptedca.feature.scanner.api

import com.engboost.encryptedca.core.network.api.entity.LocalNetwork
import com.engboost.encryptedca.feature.scanner.api.entity.ScanUpdate
import kotlinx.coroutines.flow.Flow

interface ScannerFeature {
    fun networkToScan(): Flow<LocalNetwork?>

    fun scan(wifi: LocalNetwork?): Flow<ScanUpdate>
}

package com.engboost.encryptedca.ui.scanner.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.engboost.encryptedca.ui.profiles.api.ProfilesNavRoute
import com.engboost.encryptedca.ui.scanner.impl.ui.entity.ScannerCallbacks
import com.engboost.encryptedca.ui.webpanel.api.WebPanelNavRoute
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ScannerScreen(
    navController: NavHostController,
    vm: ScannerViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val callbacks = remember(vm, navController) {
        ScannerCallbacks(
            onRescan = vm::rescan,
            onOpenProfiles = { navController.navigate(ProfilesNavRoute.ROUTE) },
            onOpenDevice = { device ->
                device.serverFingerprint?.let { navController.navigate(WebPanelNavRoute.build(device.ip, it)) }
            },
        )
    }
    ScannerScreenView(state = state, callbacks = callbacks)
}

package com.engboost.encryptedca.ui.profiles.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.engboost.encryptedca.ui.addprofile.api.AddProfileNavRoute
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesCallbacks
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ProfilesScreen(
    navController: NavHostController,
    vm: ProfilesViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val callbacks = remember(vm, navController) {
        ProfilesCallbacks(
            onBack = { navController.popBackStack() },
            onRetryLoad = vm::load,
            onSelect = vm::askToSelect,
            onDelete = vm::askToDelete,
            onConfirm = vm::confirm,
            onDismissConfirmation = vm::dismissConfirmation,
            onAddProfile = vm::showSourceSheet,
            onSourcePicked = { source ->
                vm.hideSourceSheet()
                navController.navigate(AddProfileNavRoute.build(source))
            },
            onDismissSourceSheet = vm::hideSourceSheet,
        )
    }
    ProfilesScreenView(state = state, callbacks = callbacks)
}

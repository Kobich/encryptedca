package com.engboost.encryptedca.ui.addprofile.impl.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource
import com.engboost.encryptedca.ui.addprofile.impl.R
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileCallbacks
import com.engboost.encryptedca.ui.addprofile.impl.ui.entity.AddProfileViewState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun AddProfileScreen(
    navController: NavHostController,
    source: ProfileSource,
    vm: AddProfileViewModel = koinViewModel { parametersOf(source) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val callbacks = remember(vm, navController, context) {
        AddProfileCallbacks(
            onClose = {
                if ((state as? AddProfileViewState.Form)?.importing == true) {
                    Toast.makeText(context, R.string.import_wait_before_leaving, Toast.LENGTH_SHORT).show()
                } else {
                    navController.popBackStack()
                }
            },
            onP12Picked = vm::pickP12,
            onCaPicked = vm::pickCa,
            onQrCodesScanned = vm::addQrCodes,
            onQrPhotosPicked = vm::readQrPhotos,
            onCollectQrAgain = vm::collectQrAgain,
            onImport = vm::importProfile,
        )
    }
    BackHandler(onBack = callbacks.onClose)

    val imported = (state as? AddProfileViewState.Form)?.imported == true
    LaunchedEffect(imported) {
        if (imported) navController.popBackStack()
    }

    AddProfileScreenView(state = state, callbacks = callbacks)
}

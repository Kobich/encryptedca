package com.engboost.encryptedca.ui.profiles.impl

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.engboost.encryptedca.ui.profiles.api.ProfilesUiFeature
import com.engboost.encryptedca.ui.profiles.impl.ui.ProfilesScreen

internal class ProfilesUiFeatureImpl : ProfilesUiFeature {
    @Composable
    override fun Content(navController: NavHostController) {
        ProfilesScreen(navController = navController)
    }
}

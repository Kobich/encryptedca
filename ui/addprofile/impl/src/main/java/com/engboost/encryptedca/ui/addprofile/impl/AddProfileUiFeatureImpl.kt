package com.engboost.encryptedca.ui.addprofile.impl

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.engboost.encryptedca.ui.addprofile.api.AddProfileUiFeature
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource
import com.engboost.encryptedca.ui.addprofile.impl.ui.AddProfileScreen

internal class AddProfileUiFeatureImpl : AddProfileUiFeature {
    @Composable
    override fun Content(navController: NavHostController, source: ProfileSource) {
        AddProfileScreen(navController = navController, source = source)
    }
}

package com.engboost.encryptedca.ui.addprofile.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource

interface AddProfileUiFeature {
    @Composable
    fun Content(navController: NavHostController, source: ProfileSource)
}

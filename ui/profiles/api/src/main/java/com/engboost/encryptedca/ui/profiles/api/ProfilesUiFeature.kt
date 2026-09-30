package com.engboost.encryptedca.ui.profiles.api

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

interface ProfilesUiFeature {
    @Composable
    fun Content(navController: NavHostController)
}

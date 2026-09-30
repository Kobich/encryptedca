package com.engboost.encryptedca.ui.certificates.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.engboost.encryptedca.ui.certificates.api.CertificatesUiFeature
import com.engboost.encryptedca.ui.certificates.impl.add.AddProfileScreen
import com.engboost.encryptedca.ui.certificates.impl.add.AddProfileViewModel
import com.engboost.encryptedca.ui.certificates.impl.add.ProfileSource
import com.engboost.encryptedca.ui.certificates.impl.list.ProfileListScreen
import com.engboost.encryptedca.ui.certificates.impl.list.ProfileListViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val LIST_ROUTE = "list"
private const val SOURCE_ARG = "source"
private const val ADD_ROUTE = "add/{$SOURCE_ARG}"

internal class CertificatesUiFeatureImpl : CertificatesUiFeature {

    @Composable
    override fun Content(onBack: () -> Unit) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = LIST_ROUTE) {
            composable(LIST_ROUTE) {
                val viewModel: ProfileListViewModel = koinViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                ProfileListScreen(
                    state = state,
                    onAction = viewModel::onAction,
                    onAddProfile = navController::navigateToAddProfile,
                    onBack = onBack,
                )
            }
            composable(
                route = ADD_ROUTE,
                arguments = listOf(navArgument(SOURCE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val source = ProfileSource.valueOf(requireNotNull(entry.arguments?.getString(SOURCE_ARG)))
                val viewModel: AddProfileViewModel = koinViewModel { parametersOf(source) }
                val state by viewModel.state.collectAsStateWithLifecycle()
                AddProfileScreen(
                    state = state,
                    onAction = viewModel::onAction,
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavController.navigateToAddProfile(source: ProfileSource) = navigate("add/${source.name}")

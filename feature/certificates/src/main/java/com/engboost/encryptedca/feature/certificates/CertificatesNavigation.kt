package com.engboost.encryptedca.feature.certificates

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.feature.certificates.add.AddProfileScreen
import com.engboost.encryptedca.feature.certificates.add.AddProfileViewModel
import com.engboost.encryptedca.feature.certificates.add.DocumentReader
import com.engboost.encryptedca.feature.certificates.add.ProfileImporter
import com.engboost.encryptedca.feature.certificates.add.ProfileSource
import com.engboost.encryptedca.feature.certificates.add.qr.QrImageReader
import com.engboost.encryptedca.feature.certificates.list.ProfileListScreen
import com.engboost.encryptedca.feature.certificates.list.ProfileListViewModel

const val CERTIFICATES_ROUTE = "certificates"
private const val PROFILE_LIST_ROUTE = "certificates/list"
private const val SOURCE_ARG = "source"
private const val ADD_PROFILE_ROUTE = "certificates/add/{$SOURCE_ARG}"

fun NavController.navigateToCertificates() = navigate(CERTIFICATES_ROUTE)

private fun NavController.navigateToAddProfile(source: ProfileSource) = navigate("certificates/add/${source.name}")

fun NavGraphBuilder.certificatesGraph(
    navController: NavController,
    repository: CertificateProfileRepository,
) {
    navigation(startDestination = PROFILE_LIST_ROUTE, route = CERTIFICATES_ROUTE) {
        composable(PROFILE_LIST_ROUTE) {
            val viewModel = viewModel { ProfileListViewModel(repository) }
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileListScreen(
                state = state,
                onAction = viewModel::onAction,
                onAddProfile = navController::navigateToAddProfile,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ADD_PROFILE_ROUTE,
            arguments = listOf(navArgument(SOURCE_ARG) { type = NavType.StringType }),
        ) { entry ->
            val source = ProfileSource.valueOf(requireNotNull(entry.arguments?.getString(SOURCE_ARG)))
            val appContext = LocalContext.current.applicationContext
            val viewModel = viewModel {
                val documents = DocumentReader(appContext.contentResolver)
                AddProfileViewModel(
                    documents = documents,
                    importer = ProfileImporter(repository, documents),
                    qrImages = QrImageReader(appContext),
                    savedState = createSavedStateHandle(),
                    source = source,
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            AddProfileScreen(
                state = state,
                onAction = viewModel::onAction,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

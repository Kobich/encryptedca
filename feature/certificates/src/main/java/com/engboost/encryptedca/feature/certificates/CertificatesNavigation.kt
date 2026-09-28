package com.engboost.encryptedca.feature.certificates

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.feature.certificates.add.AddProfileScreen
import com.engboost.encryptedca.feature.certificates.add.AddProfileViewModel
import com.engboost.encryptedca.feature.certificates.add.DocumentReader
import com.engboost.encryptedca.feature.certificates.list.ProfileListScreen
import com.engboost.encryptedca.feature.certificates.list.ProfileListViewModel

const val CERTIFICATES_ROUTE = "certificates"
private const val PROFILE_LIST_ROUTE = "certificates/list"
private const val ADD_PROFILE_ROUTE = "certificates/add"

fun NavController.navigateToCertificates() = navigate(CERTIFICATES_ROUTE)

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
                onAddProfile = { navController.navigate(ADD_PROFILE_ROUTE) },
                onBack = { navController.popBackStack() },
                onSelect = viewModel::requestSelect,
                onDelete = viewModel::requestDelete,
                onConfirm = viewModel::confirmPendingAction,
                onDismiss = viewModel::dismissPendingAction,
            )
        }
        composable(ADD_PROFILE_ROUTE) {
            val resolver = LocalContext.current.applicationContext.contentResolver
            val viewModel = viewModel {
                AddProfileViewModel(repository, DocumentReader(resolver), createSavedStateHandle())
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            AddProfileScreen(
                state = state,
                onP12Picked = viewModel::selectP12,
                onCaPicked = viewModel::selectCa,
                onImport = viewModel::importProfile,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

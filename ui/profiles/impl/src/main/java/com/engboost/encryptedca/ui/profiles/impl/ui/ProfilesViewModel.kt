// Select and delete are confirmed in a dialog first.
package com.engboost.encryptedca.ui.profiles.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.api.entity.ProfileSummary
import com.engboost.encryptedca.ui.profiles.impl.domain.ProfilesInteractor
import com.engboost.encryptedca.ui.profiles.impl.domain.entity.ProfilesLoad
import com.engboost.encryptedca.ui.profiles.impl.domain.entity.ProfilesState
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ConfirmationViewState
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfileViewState
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesStatus
import com.engboost.encryptedca.ui.profiles.impl.ui.entity.ProfilesViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ProfilesViewModel(
    private val interactor: ProfilesInteractor,
) : ViewModel() {

    private val confirmation = MutableStateFlow<ConfirmationViewState?>(null)
    private val sourceSheetVisible = MutableStateFlow(false)

    val state: StateFlow<ProfilesViewState> =
        combine(interactor.state, confirmation, sourceSheetVisible) { profiles, confirmation, sourceSheetVisible ->
            profiles.toViewState(confirmation, sourceSheetVisible)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfilesViewState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { interactor.load() }
    }

    fun askToSelect(profile: ProfileViewState) = askToConfirm(ConfirmationViewState.Select(profile))

    fun askToDelete(profile: ProfileViewState) = askToConfirm(ConfirmationViewState.Delete(profile))

    fun confirm() {
        val confirmed = confirmation.value ?: return
        confirmation.value = null
        viewModelScope.launch {
            when (confirmed) {
                is ConfirmationViewState.Select -> interactor.select(confirmed.profile.id)
                is ConfirmationViewState.Delete -> interactor.delete(confirmed.profile.id)
            }
        }
    }

    fun dismissConfirmation() {
        confirmation.value = null
    }

    fun showSourceSheet() {
        sourceSheetVisible.value = true
    }

    fun hideSourceSheet() {
        sourceSheetVisible.value = false
    }

    private fun askToConfirm(confirmation: ConfirmationViewState) {
        interactor.clearChangeError()
        this.confirmation.value = confirmation
    }
}

private fun ProfilesState.toViewState(
    confirmation: ConfirmationViewState?,
    sourceSheetVisible: Boolean,
): ProfilesViewState {
    val index = (load as? ProfilesLoad.Loaded)?.index
    val profiles = index?.profiles.orEmpty().map(ProfileSummary::toViewState)
    val status = when {
        load is ProfilesLoad.Failed -> ProfilesStatus.LoadFailed(load.error)
        load == ProfilesLoad.Loading || changing -> ProfilesStatus.Loading
        changeError != null -> ProfilesStatus.ChangeFailed(changeError)
        profiles.isEmpty() -> ProfilesStatus.Empty
        else -> ProfilesStatus.Ready
    }
    return ProfilesViewState(
        profiles = profiles,
        activeProfileId = index?.activeProfileId,
        status = status,
        confirmation = confirmation,
        sourceSheetVisible = sourceSheetVisible,
    )
}

private fun ProfileSummary.toViewState() = ProfileViewState(id = profileId, name = displayName, createdAt = createdAt)

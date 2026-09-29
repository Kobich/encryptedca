// The profile list: shows stored profiles, makes one active or deletes one.
// Select and delete are confirmed in a dialog first.
// A list already on screen stays when a reload fails.
package com.engboost.encryptedca.ui.certificates.impl.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.model.ProfileSummary
import com.engboost.encryptedca.feature.certificates.api.ProfileListInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileListViewModel(
    private val interactor: ProfileListInteractor,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileListState())
    val state: StateFlow<ProfileListState> = _state.asStateFlow()

    private var runningChanges = 0

    init {
        interactor.profiles
            .onEach { index ->
                _state.update {
                    it.copy(
                        profiles = index.profiles.map(ProfileSummary::toItem),
                        activeProfileId = index.activeProfileId,
                        listLoad = ListLoad.Loaded,
                    )
                }
            }
            .launchIn(viewModelScope)
        loadList()
    }

    fun onAction(action: ProfileListAction) {
        when (action) {
            is ProfileListAction.Select -> askToConfirm(PendingConfirmation.Select(action.profile))
            is ProfileListAction.Delete -> askToConfirm(PendingConfirmation.Delete(action.profile))
            ProfileListAction.Confirm -> runConfirmed()
            ProfileListAction.Dismiss -> dismissConfirmation()
            ProfileListAction.RetryLoad -> loadList()
        }
    }

    private fun loadList() {
        if (_state.value.listLoad != ListLoad.Loaded) _state.update { it.copy(listLoad = ListLoad.Loading) }
        viewModelScope.launch {
            val error = interactor.refresh() ?: return@launch
            _state.update { if (it.listLoad == ListLoad.Loaded) it else it.copy(listLoad = ListLoad.Failed(error)) }
        }
    }

    private fun askToConfirm(confirmation: PendingConfirmation) =
        _state.update { it.copy(pendingConfirmation = confirmation, changeError = null) }

    private fun dismissConfirmation() = _state.update { it.copy(pendingConfirmation = null) }

    private fun runConfirmed() {
        val confirmation = _state.value.pendingConfirmation ?: return
        dismissConfirmation()
        val profileId = confirmation.profile.id
        when (confirmation) {
            is PendingConfirmation.Select -> runChange { interactor.select(profileId) }
            is PendingConfirmation.Delete -> runChange { interactor.delete(profileId) }
        }
    }

    private fun runChange(change: suspend () -> CertificateProfileError?) {
        viewModelScope.launch {
            runningChanges++
            _state.update { it.copy(changing = true, changeError = null) }
            try {
                val error = change()
                _state.update { it.copy(changeError = error) }
            } finally {
                runningChanges--
                _state.update { it.copy(changing = runningChanges > 0) }
            }
        }
    }
}

private fun ProfileSummary.toItem() = ProfileItem(id = profileId, name = displayName, createdAt = createdAt)

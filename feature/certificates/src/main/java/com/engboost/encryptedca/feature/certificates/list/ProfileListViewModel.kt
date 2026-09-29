package com.engboost.encryptedca.feature.certificates.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError
import com.engboost.encryptedca.core.certificates.model.CertificateProfileException
import com.engboost.encryptedca.core.certificates.model.ProfileSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileListViewModel(private val repository: CertificateProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileListState())
    val state: StateFlow<ProfileListState> = _state.asStateFlow()

    // Touched only on the main thread, so a plain counter is enough.
    private var runningChanges = 0

    init {
        // Every successful read or change publishes the index; that is what marks the list as loaded.
        repository.index
            .filterNotNull()
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

    /** Also the retry after a failed read. */
    fun loadList() {
        if (_state.value.listLoad != ListLoad.Loaded) _state.update { it.copy(listLoad = ListLoad.Loading) }
        viewModelScope.launch {
            val error = errorOf { repository.refresh() } ?: return@launch
            // A list already on screen stays; only a list that never loaded shows the error.
            _state.update { if (it.listLoad == ListLoad.Loaded) it else it.copy(listLoad = ListLoad.Failed(error)) }
        }
    }

    fun requestSelect(profile: ProfileItem) = openDialog(PendingAction.Select(profile))

    fun requestDelete(profile: ProfileItem) = openDialog(PendingAction.Delete(profile))

    fun dismissPendingAction() = _state.update { it.copy(pendingAction = null) }

    fun confirmPendingAction() {
        val action = _state.value.pendingAction ?: return
        dismissPendingAction()
        val profileId = action.profile.id
        when (action) {
            is PendingAction.Select -> runChange { repository.selectProfile(profileId) }
            is PendingAction.Delete -> runChange { repository.deleteProfile(profileId) }
        }
    }

    private fun openDialog(action: PendingAction) = _state.update { it.copy(pendingAction = action, changeError = null) }

    private fun runChange(change: suspend () -> Unit) {
        viewModelScope.launch {
            runningChanges++
            _state.update { it.copy(changing = true, changeError = null) }
            try {
                val error = errorOf(change)
                _state.update { it.copy(changeError = error) }
            } finally {
                runningChanges--
                _state.update { it.copy(changing = runningChanges > 0) }
            }
        }
    }

    private suspend fun errorOf(block: suspend () -> Unit): CertificateProfileError? =
        try {
            block()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: CertificateProfileException) {
            e.error
        } catch (e: Exception) {
            CertificateProfileError.STORAGE_FAILED
        }
}

private fun ProfileSummary.toItem() = ProfileItem(id = profileId, name = displayName, createdAt = createdAt)

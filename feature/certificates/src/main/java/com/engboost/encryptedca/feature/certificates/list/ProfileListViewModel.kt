package com.engboost.encryptedca.feature.certificates.list

import androidx.compose.runtime.Immutable
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

internal data class ProfileItem(val id: String, val name: String?, val createdAt: Long)

@Immutable
internal data class ProfileListState(
    val profiles: List<ProfileItem> = emptyList(),
    val activeProfileId: String? = null,
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val error: CertificateProfileError? = null,
    val pendingAction: PendingAction? = null,
) {
    val loading: Boolean get() = !loaded || busy
}

@Immutable
internal sealed interface PendingAction {
    val profile: ProfileItem

    data class Select(override val profile: ProfileItem) : PendingAction
    data class Delete(override val profile: ProfileItem) : PendingAction
}

internal class ProfileListViewModel(private val repository: CertificateProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileListState())
    val state: StateFlow<ProfileListState> = _state.asStateFlow()

    // Touched only on the main thread, so a plain counter is enough.
    private var runningChanges = 0

    init {
        repository.index
            .filterNotNull()
            .onEach { index ->
                _state.update {
                    it.copy(
                        profiles = index.profiles.map(ProfileSummary::toItem),
                        activeProfileId = index.activeProfileId,
                        loaded = true,
                    )
                }
            }
            .launchIn(viewModelScope)
        launchChange { repository -> repository.refresh() }
    }

    fun requestSelect(profile: ProfileItem) = openDialog(PendingAction.Select(profile))

    fun requestDelete(profile: ProfileItem) = openDialog(PendingAction.Delete(profile))

    fun dismissPendingAction() = _state.update { it.copy(pendingAction = null, error = null) }

    fun confirmPendingAction() {
        val action = _state.value.pendingAction ?: return
        dismissPendingAction()
        val profileId = action.profile.id
        when (action) {
            is PendingAction.Select -> launchChange { repository -> repository.selectProfile(profileId) }
            is PendingAction.Delete -> launchChange { repository -> repository.deleteProfile(profileId) }
        }
    }

    private fun openDialog(action: PendingAction) = _state.update { it.copy(pendingAction = action, error = null) }

    private fun launchChange(change: suspend (CertificateProfileRepository) -> Unit) {
        viewModelScope.launch {
            runningChanges++
            _state.update { it.copy(busy = true, error = null) }
            try {
                val error = errorOf { change(repository) }
                _state.update { it.copy(error = error) }
            } finally {
                runningChanges--
                _state.update { it.copy(busy = runningChanges > 0) }
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

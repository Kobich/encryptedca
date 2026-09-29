// Список профилей: показывает сохранённые профили, делает один из них активным или удаляет.
// Выбор и удаление сначала подтверждаются диалогом, потом выполняются в репозитории.
// Если список уже на экране, ошибка повторной загрузки его не прячет.
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

    private var runningChanges = 0

    init {
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
            val error = errorOf { repository.refresh() } ?: return@launch
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
            is PendingConfirmation.Select -> runChange { repository.selectProfile(profileId) }
            is PendingConfirmation.Delete -> runChange { repository.deleteProfile(profileId) }
        }
    }

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

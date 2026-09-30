package com.engboost.encryptedca.ui.profiles.impl.ui.entity

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError

internal data class ProfileViewState(val id: String, val name: String?, val createdAt: Long)

@Immutable
internal sealed interface ProfilesStatus {
    data object Loading : ProfilesStatus
    data class LoadFailed(val error: CertificateProfileError) : ProfilesStatus
    data class ChangeFailed(val error: CertificateProfileError) : ProfilesStatus
    data object Empty : ProfilesStatus
    data object Ready : ProfilesStatus
}

@Immutable
internal sealed interface ConfirmationViewState {
    val profile: ProfileViewState

    data class Select(override val profile: ProfileViewState) : ConfirmationViewState
    data class Delete(override val profile: ProfileViewState) : ConfirmationViewState
}

@Immutable
internal data class ProfilesViewState(
    val profiles: List<ProfileViewState> = emptyList(),
    val activeProfileId: String? = null,
    val status: ProfilesStatus = ProfilesStatus.Loading,
    val confirmation: ConfirmationViewState? = null,
    val sourceSheetVisible: Boolean = false,
)

package com.engboost.encryptedca.feature.certificates.impl.presentation.list

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.core.certificates.api.model.CertificateProfileError

data class ProfileItem(val id: String, val name: String?, val createdAt: Long)

@Immutable
sealed interface ListLoad {
    data object Loading : ListLoad
    data object Loaded : ListLoad
    data class Failed(val error: CertificateProfileError) : ListLoad
}

@Immutable
sealed interface PendingConfirmation {
    val profile: ProfileItem

    data class Select(override val profile: ProfileItem) : PendingConfirmation
    data class Delete(override val profile: ProfileItem) : PendingConfirmation
}

@Immutable
data class ProfileListState(
    val profiles: List<ProfileItem> = emptyList(),
    val activeProfileId: String? = null,
    val listLoad: ListLoad = ListLoad.Loading,
    val changing: Boolean = false,
    val changeError: CertificateProfileError? = null,
    val pendingConfirmation: PendingConfirmation? = null,
)

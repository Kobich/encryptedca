package com.engboost.encryptedca.feature.certificates.list

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.core.certificates.model.CertificateProfileError

internal data class ProfileItem(val id: String, val name: String?, val createdAt: Long)

/** Reading the profile list itself, separate from selecting or deleting a profile. */
@Immutable
internal sealed interface ListLoad {
    data object Loading : ListLoad
    data object Loaded : ListLoad
    data class Failed(val error: CertificateProfileError) : ListLoad
}

/** A select or delete the user still has to confirm. */
@Immutable
internal sealed interface PendingAction {
    val profile: ProfileItem

    data class Select(override val profile: ProfileItem) : PendingAction
    data class Delete(override val profile: ProfileItem) : PendingAction
}

@Immutable
internal data class ProfileListState(
    val profiles: List<ProfileItem> = emptyList(),
    val activeProfileId: String? = null,
    val listLoad: ListLoad = ListLoad.Loading,
    /** A confirmed select or delete is running. */
    val changing: Boolean = false,
    val changeError: CertificateProfileError? = null,
    val pendingAction: PendingAction? = null,
)

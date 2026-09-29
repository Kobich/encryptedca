package com.engboost.encryptedca.ui.certificates.impl.list

internal sealed interface ProfileListAction {
    data class Select(val profile: ProfileItem) : ProfileListAction
    data class Delete(val profile: ProfileItem) : ProfileListAction
    data object Confirm : ProfileListAction
    data object Dismiss : ProfileListAction
    data object RetryLoad : ProfileListAction
}

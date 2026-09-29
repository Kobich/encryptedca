package com.engboost.encryptedca.feature.certificates.impl.presentation.list

sealed interface ProfileListAction {
    data class Select(val profile: ProfileItem) : ProfileListAction
    data class Delete(val profile: ProfileItem) : ProfileListAction
    data object Confirm : ProfileListAction
    data object Dismiss : ProfileListAction
    data object RetryLoad : ProfileListAction
}

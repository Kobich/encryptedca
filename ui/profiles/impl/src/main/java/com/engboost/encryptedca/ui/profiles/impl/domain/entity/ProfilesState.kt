package com.engboost.encryptedca.ui.profiles.impl.domain.entity

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex

internal sealed interface ProfilesLoad {
    data object Loading : ProfilesLoad
    data class Loaded(val index: ProfileIndex) : ProfilesLoad
    data class Failed(val error: CertificateProfileError) : ProfilesLoad
}

internal data class ProfilesState(
    val load: ProfilesLoad = ProfilesLoad.Loading,
    val changing: Boolean = false,
    val changeError: CertificateProfileError? = null,
)

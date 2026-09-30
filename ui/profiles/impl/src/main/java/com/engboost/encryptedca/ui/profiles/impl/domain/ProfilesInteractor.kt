// Stored profiles: loading the list, making one active, deleting one.
// A list already loaded stays when a reload fails.
package com.engboost.encryptedca.ui.profiles.impl.domain

import com.engboost.encryptedca.core.certificates.api.entity.CertificateProfileError
import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex
import com.engboost.encryptedca.feature.certificates.api.CertificatesFeature
import com.engboost.encryptedca.ui.profiles.impl.domain.entity.ProfilesLoad
import com.engboost.encryptedca.ui.profiles.impl.domain.entity.ProfilesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

internal class ProfilesInteractor(
    private val certificatesFeature: CertificatesFeature,
) {
    private val index: Flow<ProfileIndex?> = certificatesFeature.profiles.onStart { emit(null) }
    private val loadError = MutableStateFlow<CertificateProfileError?>(null)
    private val runningChanges = MutableStateFlow(0)
    private val changeError = MutableStateFlow<CertificateProfileError?>(null)

    val state: Flow<ProfilesState> =
        combine(index, loadError, runningChanges, changeError) { index, loadError, runningChanges, changeError ->
            ProfilesState(
                load = when {
                    index != null -> ProfilesLoad.Loaded(index)
                    loadError != null -> ProfilesLoad.Failed(loadError)
                    else -> ProfilesLoad.Loading
                },
                changing = runningChanges > 0,
                changeError = changeError,
            )
        }

    suspend fun load() {
        loadError.value = null
        loadError.value = certificatesFeature.refreshProfiles()
    }

    suspend fun select(profileId: String) = change { certificatesFeature.selectProfile(profileId) }

    suspend fun delete(profileId: String) = change { certificatesFeature.deleteProfile(profileId) }

    fun clearChangeError() {
        changeError.value = null
    }

    private suspend fun change(block: suspend () -> CertificateProfileError?) {
        runningChanges.update { it + 1 }
        changeError.value = null
        try {
            changeError.value = block()
        } finally {
            runningChanges.update { it - 1 }
        }
    }
}

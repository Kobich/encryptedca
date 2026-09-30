package com.engboost.encryptedca.core.certificates.impl.domain

import com.engboost.encryptedca.core.certificates.api.entity.ProfileIndex

internal interface ProfileIndexRepository {
    val activeProfileId: String?

    fun read(): ProfileIndex

    fun contains(profileId: String): Boolean

    fun setActive(profileId: String)

    fun register(profileId: String, displayName: String?)

    fun unregister(profileId: String)
}

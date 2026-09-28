package com.engboost.encryptedca.core.certificates

data class ProfileIndex(
    val profiles: List<ProfileSummary>,
    val activeProfileId: String?,
)

data class ProfileSummary(
    val profileId: String,
    val displayName: String?,
    /** Epoch millis; 0 for profiles imported before the timestamp was stored. */
    val createdAt: Long,
)

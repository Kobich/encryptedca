package com.engboost.encryptedca.core.certificates.model

data class ProfileIndex(
    val profiles: List<ProfileSummary>,
    val activeProfileId: String?,
)

data class ProfileSummary(
    val profileId: String,
    val displayName: String?,
    val createdAt: Long,
)

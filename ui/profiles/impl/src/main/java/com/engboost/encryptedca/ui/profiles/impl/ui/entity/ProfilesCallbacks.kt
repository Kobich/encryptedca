package com.engboost.encryptedca.ui.profiles.impl.ui.entity

import com.engboost.encryptedca.ui.addprofile.api.entity.ProfileSource

internal data class ProfilesCallbacks(
    val onBack: () -> Unit,
    val onRetryLoad: () -> Unit,
    val onSelect: (ProfileViewState) -> Unit,
    val onDelete: (ProfileViewState) -> Unit,
    val onConfirm: () -> Unit,
    val onDismissConfirmation: () -> Unit,
    val onAddProfile: () -> Unit,
    val onSourcePicked: (ProfileSource) -> Unit,
    val onDismissSourceSheet: () -> Unit,
)

package com.engboost.encryptedca.feature.webpanel.impl.presentation

import androidx.compose.runtime.Immutable

enum class WebPanelProblem {
    NO_PROFILE,
    PROFILE_UNAVAILABLE,
    UNTRUSTED_SERVER,
}

@Immutable
sealed interface ImageMessage {
    val id: Long

    data class Saved(override val id: Long, val uri: String) : ImageMessage
    data class Failed(override val id: Long) : ImageMessage
}

@Immutable
data class WebPanelState(
    val host: String,
    val startUrl: String,
    val pageUrl: String = startUrl,
    val loading: Boolean = true,
    val problem: WebPanelProblem? = null,
    val imageMessage: ImageMessage? = null,
)

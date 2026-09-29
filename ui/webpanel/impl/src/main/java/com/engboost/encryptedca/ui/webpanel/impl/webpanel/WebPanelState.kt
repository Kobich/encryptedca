package com.engboost.encryptedca.ui.webpanel.impl.webpanel

import androidx.compose.runtime.Immutable

internal enum class WebPanelProblem {
    NO_PROFILE,
    PROFILE_UNAVAILABLE,
    UNTRUSTED_SERVER,
}

@Immutable
internal sealed interface ImageMessage {
    val id: Long

    data class Saved(override val id: Long, val uri: String) : ImageMessage
    data class Failed(override val id: Long) : ImageMessage
}

@Immutable
internal data class WebPanelState(
    val host: String,
    val startUrl: String,
    val pageUrl: String = startUrl,
    val loading: Boolean = true,
    val problem: WebPanelProblem? = null,
    val imageMessage: ImageMessage? = null,
)

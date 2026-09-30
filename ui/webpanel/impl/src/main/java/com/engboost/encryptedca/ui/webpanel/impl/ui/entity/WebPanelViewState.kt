package com.engboost.encryptedca.ui.webpanel.impl.ui.entity

import androidx.compose.runtime.Immutable
import com.engboost.encryptedca.ui.webpanel.impl.domain.entity.WebPanelProblem

@Immutable
internal sealed interface ImageMessage {
    val id: Long

    data class Saved(override val id: Long, val uri: String) : ImageMessage
    data class Failed(override val id: Long) : ImageMessage
}

@Immutable
internal data class WebPanelViewState(
    val host: String,
    val startUrl: String,
    val pageUrl: String = startUrl,
    val loading: Boolean = true,
    val problem: WebPanelProblem? = null,
    val imageMessage: ImageMessage? = null,
)

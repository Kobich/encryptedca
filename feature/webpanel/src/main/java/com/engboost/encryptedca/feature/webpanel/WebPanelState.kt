package com.engboost.encryptedca.feature.webpanel

import androidx.compose.runtime.Immutable

internal enum class WebPanelProblem {
    /** No profile is selected, so the device can't be asked to accept us. */
    NO_PROFILE,
    /** A profile is selected, but its key or CA couldn't be read. */
    PROFILE_UNAVAILABLE,
    /** The device presented a certificate other than the one the scan verified. */
    UNTRUSTED_SERVER,
}

/**
 * The result of saving a screenshot, shown once. A newer result replaces one that hasn't been shown
 * yet; [id] tells two equal results apart, so each is shown.
 */
@Immutable
internal sealed interface ImageMessage {
    val id: Long

    data class Saved(override val id: Long, val uri: String) : ImageMessage
    data class Failed(override val id: Long) : ImageMessage
}

@Immutable
internal data class WebPanelState(
    /** The device's IP: the only host the panel trusts and shows. */
    val host: String,
    val startUrl: String,
    /** The page shown now; changes as the user navigates inside the panel. */
    val pageUrl: String = startUrl,
    val loading: Boolean = true,
    /** Belongs to the current load; a reload clears it. */
    val problem: WebPanelProblem? = null,
    val imageMessage: ImageMessage? = null,
)

package com.engboost.encryptedca.feature.certificates.api.entity

sealed interface QrCollectResult {
    data object NothingNew : QrCollectResult
    data object Collecting : QrCollectResult
    data object Complete : QrCollectResult
    data class Invalid(val cause: Throwable) : QrCollectResult
}

package com.engboost.encryptedca.ui.scanner.impl.ui.entity

internal data class ScannerCallbacks(
    val onRescan: () -> Unit,
    val onOpenProfiles: () -> Unit,
    val onOpenDevice: (DeviceViewState) -> Unit,
)

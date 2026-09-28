package com.engboost.encryptedca.core.certificates.storage

import java.security.KeyStore

internal const val ANDROID_KEY_STORE = "AndroidKeyStore"

internal fun openAndroidKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

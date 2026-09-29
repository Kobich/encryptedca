package com.engboost.encryptedca

import android.app.Application
import android.util.Log
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EncryptedCaApplication : Application() {
    val certificateProfiles by lazy { CertificateProfileRepository(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Reads the profile index while the first screen is drawn, so the certificate list opens already filled.
        appScope.launch {
            try {
                certificateProfiles.refresh()
            } catch (e: Exception) {
                // The list screen retries and shows the error.
                Log.w("EncryptedCa", "Could not preload certificate profiles", e)
            }
        }
    }
}

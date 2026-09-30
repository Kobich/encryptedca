// Starts Koin and reads the profile list at startup, so the certificates screen opens already filled.
// If the read fails, that screen shows the error and offers a retry.
package com.engboost.encryptedca

import android.app.Application
import android.util.Log
import com.engboost.encryptedca.core.certificates.api.ProfileStorageFeature
import com.engboost.encryptedca.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EncryptedCaApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EncryptedCaApplication)
            modules(appModules)
        }
        val profiles: ProfileStorageFeature = get()
        appScope.launch {
            try {
                profiles.refresh()
            } catch (e: Exception) {
                Log.w("EncryptedCa", "Could not preload certificate profiles", e)
            }
        }
    }
}

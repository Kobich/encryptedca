// Приложение заранее читает список профилей, чтобы экран сертификатов открылся уже заполненным.
// Если чтение не удалось, ошибку покажет сам экран и предложит повторить.
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
        appScope.launch {
            try {
                certificateProfiles.refresh()
            } catch (e: Exception) {
                Log.w("EncryptedCa", "Could not preload certificate profiles", e)
            }
        }
    }
}

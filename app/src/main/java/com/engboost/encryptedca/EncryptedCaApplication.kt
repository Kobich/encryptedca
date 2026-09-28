package com.engboost.encryptedca

import android.app.Application
import com.engboost.encryptedca.core.certificates.CertificateProfileRepository

class EncryptedCaApplication : Application() {
    val certificateProfiles by lazy { CertificateProfileRepository(this) }
}

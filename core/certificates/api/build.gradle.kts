plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.engboost.encryptedca.core.certificates.api"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.kotlinx.coroutines.android)
}

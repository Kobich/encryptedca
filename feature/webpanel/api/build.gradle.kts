plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.engboost.encryptedca.feature.webpanel.api"
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
    api(libs.navigation.runtime)
}

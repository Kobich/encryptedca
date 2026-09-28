plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.engboost.encryptedca.core.network"
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
    api(project(":core:certificates"))

    implementation(libs.kotlinx.coroutines.android)
}

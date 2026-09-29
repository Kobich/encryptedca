plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.engboost.encryptedca.feature.webpanel.impl"
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
    api(project(":core:certificates:api"))
    implementation(project(":core:network:api"))

    api(libs.koin.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
}

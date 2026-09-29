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
    implementation(project(":feature:webpanel:api"))
    implementation(project(":core:certificates:api"))
    implementation(project(":core:network:api"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}

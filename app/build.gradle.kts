plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.engboost.encryptedca"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.engboost.encryptedca"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        // Phones only: x86 emulator builds of the ML Kit QR library would add ~12 MB.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            // R8 stays off, as before. The optimization { } block needs AGP 9.3, so the classic switch is used.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:certificates:api"))
    implementation(project(":core:certificates:impl"))
    implementation(project(":core:network:impl"))

    implementation(project(":feature:certificates:impl"))
    implementation(project(":feature:scanner:impl"))
    implementation(project(":feature:webpanel:impl"))

    implementation(project(":ui:certificates:api"))
    implementation(project(":ui:certificates:impl"))
    implementation(project(":ui:scanner:api"))
    implementation(project(":ui:scanner:impl"))
    implementation(project(":ui:webpanel:api"))
    implementation(project(":ui:webpanel:impl"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
}

buildscript {
    dependencies {
        // AGP 9 bundles KGP 2.2.10; pin the version so it matches the Compose compiler plugin.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

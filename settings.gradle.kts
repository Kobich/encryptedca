pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "encryptedca"
include(":app")

include(":core:certificates:api")
include(":core:certificates:impl")
include(":core:network:api")
include(":core:network:impl")

include(":feature:certificates:api")
include(":feature:certificates:impl")
include(":feature:certificates:ui:api")
include(":feature:certificates:ui:impl")

include(":feature:scanner:api")
include(":feature:scanner:impl")
include(":feature:scanner:ui:api")
include(":feature:scanner:ui:impl")

include(":feature:webpanel:api")
include(":feature:webpanel:impl")
include(":feature:webpanel:ui:api")
include(":feature:webpanel:ui:impl")

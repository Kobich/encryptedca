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
include(":feature:scanner:api")
include(":feature:scanner:impl")
include(":feature:webpanel:api")
include(":feature:webpanel:impl")

include(":ui:scanner:api")
include(":ui:scanner:impl")
include(":ui:profiles:api")
include(":ui:profiles:impl")
include(":ui:addprofile:api")
include(":ui:addprofile:impl")
include(":ui:webpanel:api")
include(":ui:webpanel:impl")

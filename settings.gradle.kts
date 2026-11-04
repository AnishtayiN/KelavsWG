@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.dagger.hilt.android") {
                useModule("com.google.dagger:hilt-android-gradle-plugin:2.52")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KelavsVPN"

include(":tunnel")
include(":ui")
include(":core:subscription")
include(":core:update")
include(":core:network")
include(":core:storage")
include(":core:vpn")
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:servers")
include(":feature:subscreen")
project(":feature:subscreen").projectDir = file("feature/subscription")
include(":feature:settings")
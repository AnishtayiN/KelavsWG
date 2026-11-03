@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id("com.github.ben-manes.versions") version "0.53.0" apply false
}

allprojects {
    group = "com.kelavs.vpn"
    version = providers.gradleProperty("kelavsVersionName").get()
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
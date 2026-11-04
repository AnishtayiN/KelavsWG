@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "com.kelavs.vpn"
    version = providers.gradleProperty("kelavsVersionName").get()
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

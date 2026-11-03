@file:Suppress("UnstableApiUsage")

val pkg: String = providers.gradleProperty("kelavsPackageName").get()

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    compileSdk = 36
    namespace = "${pkg}.subscription_ui"
    defaultConfig {
        minSdk = 28
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.1"
    }
    kotlinOptions {
        jvmTarget = "17"
        
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
    implementation(project(":core:subscription"))
    implementation(project(":core:storage"))
    implementation(project(":core:update"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    debugImplementation(libs.compose.ui.tooling)
}
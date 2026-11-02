@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.bundling.Zip

val pkg: String = providers.gradleProperty("kelavsPackageName").get()

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
}

android {
    compileSdk = 36
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "${pkg}.tunnel"
    defaultConfig {
        minSdk = 28
    }
    externalNativeBuild {
        cmake {
            path("tools/CMakeLists.txt")
        }
    }
    testOptions.unitTests.all {
        it.testLogging { events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED) }
    }
    buildTypes {
        all {
            externalNativeBuild {
                cmake {
                    targets("libwg-go.so", "libwg.so", "libwg-quick.so")
                    arguments("-DGRADLE_USER_HOME=${project.gradle.gradleUserHomeDir}")
                    arguments("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
                }
            }
        }
        release {
            externalNativeBuild {
                cmake {
                    arguments("-DANDROID_PACKAGE_NAME=${pkg}")
                }
            }
        }
        debug {
            externalNativeBuild {
                cmake {
                    arguments("-DANDROID_PACKAGE_NAME=${pkg}.debug")
                }
            }
        }
    }
    lint {
        disable += "LongLogTag"
        disable += "NewApi"
    }
    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.collection)
    compileOnly(libs.jsr305)
    testImplementation(libs.junit)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = pkg
            artifactId = "tunnel"
            version = providers.gradleProperty("kelavsVersionName").get()
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name = "Kelavs VPN Tunnel Library"
                description = "Embeddable tunnel library for Kelavs VPN for Android"
                url = "https://github.com/AnishtayiN/KelavsWG"

                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/AnishtayiN/KelavsWG"
                    developerConnection = "scm:git:https://github.com/AnishtayiN/KelavsWG"
                    url = "https://github.com/AnishtayiN/KelavsWG"
                }
                developers {
                    organization {
                        name = "Kelavs VPN"
                        url = "https://github.com/AnishtayiN/KelavsWG"
                    }
                    developer {
                        name = "Kelavs VPN Team"
                        email = "support@kelavs.vpn"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "SonatypeUpload"
            setUrl(layout.buildDirectory.dir("sonatype"))
        }
    }
}

val releasePublication = publishing.publications.named("release").get() as MavenPublication
val mavenGroupPath = releasePublication.groupId.replace('.', '/')
val mavenArtifactId = releasePublication.artifactId
val mavenVersion = releasePublication.version

tasks.register<Zip>("zipReleasePublication") {
    dependsOn(tasks.named("publishReleasePublicationToSonatypeUploadRepository"))
    group = "distribution"
    description = "Zips the release publication in Maven repository layout."

    val sourceDir = layout.buildDirectory.dir("sonatype/${mavenGroupPath}/${mavenArtifactId}/${mavenVersion}")
    from(sourceDir)
    archiveFileName.set("${mavenArtifactId}-${mavenVersion}-maven.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    into("${mavenGroupPath}/${mavenArtifactId}/${mavenVersion}")
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
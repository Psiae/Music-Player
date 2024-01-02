pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs") {
            val compose_ui_version = "1.5.0-beta01"
            library(
                "koin.android",
                "io.insert-koin",
                "koin-android"
            ).version("3.3.3")
            library(
                "koin-androidx.compose",
                "io.insert-koin",
                "koin-androidx-compose"
            ).version("3.4.2")

            library(
                "androidx.core.core-ktx",
                "androidx.core",
                "core-ktx"
            ).version("1.9.0")

            library(
                "androidx.activity.activity-compose",
                "androidx.activity",
                "activity-compose"
            ).version("1.6.1")


            // androidx.compose
            library(
                "androidx.compose.ui-ui",
                "androidx.compose.ui",
                "ui"
            ).version(compose_ui_version)
            library(
                "androidx.compose.ui.ui-tooling",
                "androidx.compose.ui",
                "ui-tooling"
            ).version(compose_ui_version)
            library(
                "androidx.compose.ui.ui-tooling-preview",
                "androidx.compose.ui",
                "ui-tooling-preview"
            ).version(compose_ui_version)
            library(
                "androidx.compose.ui.ui-util",
                "androidx.compose.ui",
                "ui-util"
            ).version(compose_ui_version)
            library(
                "androidx.compose.foundation-foundation",
                "androidx.compose.foundation",
                "foundation"
            ).version(compose_ui_version)
            library(
                "androidx.compose.foundation-foundation-layout",
                "androidx.compose.foundation",
                "foundation-layout"
            ).version(compose_ui_version)
            library(
                "androidx.compose.material.material",
                "androidx.compose.material",
                "material"
            ).version(compose_ui_version)
            library(
                "androidx.compose.material3.material3",
                "androidx.compose.material3",
                "material3"
            ).version("1.1.1")

            library(
                "io.coil-kt.coil",
                "io.coil-kt",
                "coil"
            ).version("2.3.0")
            library(
                "io.coil-kt.coil-compose",
                "io.coil-kt",
                "coil-compose"
            ).version("2.3.0")
            library(
                "io.coil.kt.coil-svg",
                "io.coil-kt",
                "coil-svg"
            ).version("2.3.0")

            library(
                "org.jetbrains.kotlinx.kotlinx-collections-immutable",
                "org.jetbrains.kotlinx",
                "kotlinx-collections-immutable"
            ).version("0.3.5")

            library(
                "org.jetbrains.kotlinx.atomicfu-jvm",
                "org.jetbrains.kotlinx",
                "atomicfu-jvm"
            ).version("0.20.2")

            library(
                "org.jetbrains.kotlinx.kotlinx-datetime",
                "org.jetbrains.kotlinx",
                "kotlinx-datetime"
            ).version("0.4.0")

            library(
                "io.realm.kotlin.library-base",
                "io.realm.kotlin",
                "library-base"
            ).version("1.13.0")
        }
    }
}

rootProject.name = "MusicPlayer"

include(":app")

include(":core")
include(":core:core-jvm")
include(":core:core-android")


include(":base")
include(":base:base-common-jvm")
include(":base:base-common-android")

include(":base:media")
include(":base:media:medialib")
include(":base:media:medialib:lint")

include(":base:navigation")
include(":base:navigation:compose")


include(":medialib")
include(":medialib:medialib-jvm")

include(":media")
include(":media:media-android")

include(":feature:library")
include(":feature:user")
include(":feature:home")
include(":feature:search")
include(":feature:base")
include(":feature:player")
include(":media:media-jvm")

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
}

rootProject.name = "MusicPlayer"
include(":app")
include(":base")
include(":base:common")
include(":base:common:android")
include(":base:common:android:kotlin")
include(":base:common:androidx")
include(":base:common:androidx:viewmodel")
include(":base:common:androidx:viewmodel:compose")
include(":base:common:androidx:lifecycle")
include(":base:common:kotlin")
include(":base:compose")
include(":base:lifecycle")
include(":base:media")
include(":base:media:medialib")
include(":base:media:medialib:lint")
include(":core")
include(":feature:library")
include(":feature:user")
include(":feature:home")
include(":feature:search")
include(":feature:base")
include(":feature:player")

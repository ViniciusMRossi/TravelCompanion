pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// The build pins a JDK 17 toolchain (docs/technical/DEPENDENCY-BASELINE.md).
// Android Studio currently bundles JDK 21, so without a toolchain resolver the
// build fails on machines that have no separate JDK 17 installed.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TravelCompanion"
include(":app")

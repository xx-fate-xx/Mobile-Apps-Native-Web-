pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KmpPractice4_1"

// Task 17
include(":shared")
include(":androidApp")
include(":desktopApp")

// Task 19
include(":feature:auth")
include(":feature:profile")
include(":umbrella")

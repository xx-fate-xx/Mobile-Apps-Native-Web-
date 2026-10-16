import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

// Task 24
group = "uz.ttpu.shared"
version = "1.0.0"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        // without this only the jvm/ios parts are published, not android
        publishLibraryVariants("release")
    }

    // desktop
    jvm()

    // Task 25
    val xcf = XCFramework("Shared")
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            xcf.add(this)
        }
    }

    // Task 9
    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.3.0")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.3.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.3.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "uz.ttpu.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Task 24
publishing {
    repositories {
        mavenLocal()
    }
}

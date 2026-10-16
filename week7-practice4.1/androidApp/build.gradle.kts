import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "uz.ttpu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.ttpu.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // Task 17
    implementation(project(":shared"))
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
}

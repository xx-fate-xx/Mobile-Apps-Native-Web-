plugins {
    kotlin("multiplatform")
}

// Task 19 - umbrella module, iOS gets only this one framework
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":feature:auth"))
            export(project(":feature:profile"))
        }
    }

    sourceSets.commonMain.dependencies {
        // api, not implementation, otherwise export() doesn't work (Task 20)
        api(project(":feature:auth"))
        api(project(":feature:profile"))
    }
}

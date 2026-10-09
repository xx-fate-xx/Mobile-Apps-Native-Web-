plugins {
    kotlin("multiplatform")
}

kotlin {
    // Q8
    jvm()
    iosArm64()

    // Q12
    js {
        browser()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.register("printSourceSets") {
    doLast {
        kotlin.sourceSets.names.sorted().forEach { println(it) }
    }
}

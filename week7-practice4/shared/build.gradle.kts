plugins {
    kotlin("multiplatform")
}

kotlin {
    // Q16
    jvm {
        // Q21
        val main = compilations.getByName("main")
        println("jvm main compilation: " + main.name)
    }
    js(IR) {
        browser()
    }
    linuxX64()

    // Q17
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // Q16 - custom source set only for jvm + js
        val jvmAndJsMain by creating {
        }
        jvmAndJsMain.dependsOn(commonMain.get())
        jvmMain.get().dependsOn(jvmAndJsMain)
        jsMain.get().dependsOn(jvmAndJsMain)
    }
}

tasks.register("printSourceSets") {
    doLast {
        kotlin.sourceSets.names.sorted().forEach { println(it) }
    }
}

// Q21 - all compilations of the jvm target
tasks.register("printJvmCompilations") {
    doLast {
        kotlin.jvm().compilations.forEach { compilation ->
            println(compilation.name + " -> " + compilation.allKotlinSourceSets.map { it.name })
        }
    }
}

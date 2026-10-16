plugins {
    kotlin("jvm")
    application
}

dependencies {
    // Task 17
    implementation(project(":shared"))
}

application {
    mainClass.set("uz.ttpu.desktop.MainKt")
}

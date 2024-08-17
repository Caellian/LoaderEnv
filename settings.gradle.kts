pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "loaderenv"

fun addProject(name: String) {
    include(name)
    project(":${name}").projectDir = file("modules/${name}")
}

addProject("lib")
addProject("processor")
addProject("gradle")

addProject("jetbrains")

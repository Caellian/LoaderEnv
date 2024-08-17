plugins {
    id("org.jetbrains.intellij.platform") version "2.0.0"
}

version = "0.0.1"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(project(":lib"))

    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

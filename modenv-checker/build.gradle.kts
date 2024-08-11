plugins {
    application
}

application {
    mainClass = "net.tinsvagelj.mc.modenv.checker.Main"
}

group = "net.tinsvagelj.mc.modenv"
version = "0.0.1"

dependencies {
    implementation(project(":modenv-lib"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

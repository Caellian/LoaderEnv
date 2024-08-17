plugins {
    id("com.gradle.plugin-publish") version "1.2.1"
}

version = "0.0.2"

gradlePlugin {
    plugins {
        create("loaderEnvPlugin") {
            id = "net.tinsvagelj.mc.loaderenv"
            implementationClass = "net.tinsvagelj.mc.loaderenv.gradle.LoaderEnvGradlePlugin"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
}

packageInfo {
    name = "LoaderEnv Gradle Plugin"
    description = "Gradle plugin that sets up projects to use LoaderEnv library and processor"
}

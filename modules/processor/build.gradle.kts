plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
}

dependencies {
    implementation(project(":lib"))

    if (!JavaVersion.current().isJava9Compatible) {
        compileOnly(file( "${System.getProperty("java.home")}/../lib/tools.jar" ))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

packageInfo {
    name = "LoaderEnv Processor"
    description = "Java compiler plugin that removes class elements in accordance with LoaderEnv annotations"
}

moduleOptions {
    exports = listOf(
        "jdk.compiler/com.sun.tools.javac.api",
        "jdk.compiler/com.sun.tools.javac.code",
        "jdk.compiler/com.sun.tools.javac.file",
        "jdk.compiler/com.sun.tools.javac.main",
        "jdk.compiler/com.sun.tools.javac.model",
        "jdk.compiler/com.sun.tools.javac.parser",
        "jdk.compiler/com.sun.tools.javac.processing",
        "jdk.compiler/com.sun.tools.javac.tree",
        "jdk.compiler/com.sun.tools.javac.util",
    )
}

plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

group = "net.tinsvagelj.mc.modenv"
version = "0.0.1"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven("https://maven.fabricmc.net")
}

dependencies {
    implementation(project(":modenv-lib"))

    if (!JavaVersion.current().isJava9Compatible) {
        compileOnly(file( "${System.getProperty("java.home")}/../lib/tools.jar" ))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

if (JavaVersion.current().isJava9Compatible) {
    fun exports(target: String): List<String> {
        return listOf(
            "--add-exports=jdk.compiler/com.sun.tools.javac.api=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.code=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.file=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.main=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.model=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.parser=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.processing=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.tree=${target}",
            "--add-exports=jdk.compiler/com.sun.tools.javac.util=${target}",
        );
    }

    tasks.withType(JavaCompile::class) {
        options.compilerArgs.addAll(exports("modenv.processor"))
    }
}

plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("modEnvPlugin") {
            id = "net.tinsvagelj.mc.modenv"
            implementationClass = "net.tinsvagelj.mc.modenv.gradle.ModEnvGradlePlugin"
        }
    }
}

group = "net.tinsvagelj.mc.modenv"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
}

val copyJarsToResources by tasks.registering(Copy::class) {
    from(project(":modenv-lib").tasks.named("jar", Jar::class))
    from(project(":modenv-processor").tasks.named("jar", Jar::class))
    from(project(":modenv-checker").tasks.named("jar", Jar::class))
    into(layout.buildDirectory.dir("resources/main/libs"))
    rename { fileName ->
        fileName.replace("-", "_")
    }
}

val generateBundleManifest by tasks.registering {
    val versions = listOf(
        "modenv-lib" to project(":modenv-lib").tasks.named("jar", Jar::class).get().archiveFileName.get(),
        "modenv-processor" to project(":modenv-processor").tasks.named("jar", Jar::class).get().archiveFileName.get(),
        "modenv-checker" to project(":modenv-checker").tasks.named("jar", Jar::class).get().archiveFileName.get(),
    )

    doLast {
        val metaInfDir = layout.buildDirectory.dir("resources/main/META-INF").get().asFile
        metaInfDir.mkdirs()
        val versionsFile = metaInfDir.resolve("bundled-libs.yml")

        versionsFile.printWriter().use { writer ->
            versions.forEach { (projectName, version) ->
                writer.println("$projectName: $version")
            }
        }
    }
}

val generateBundleMap by tasks.registering {
    val generatedSrcDir = layout.buildDirectory.dir("generated/sources/data").get().asFile
    outputs.dir(generatedSrcDir)
    doLast {
        val versions = listOf(
            "modenv-lib" to project(":modenv-lib").tasks.named("jar", Jar::class).get().archiveFileName.get().replace("-", "_"),
            "modenv-processor" to project(":modenv-processor").tasks.named("jar", Jar::class).get().archiveFileName.get().replace("-", "_"),
            "modenv-checker" to project(":modenv-checker").tasks.named("jar", Jar::class).get().archiveFileName.get().replace("-", "_"),
        )

        val packageName = "net.tinsvagelj.mc.modenv.gradle"
        val className = "Data"

        val fileContent = buildString {
            appendLine("package $packageName;")
            appendLine()
            appendLine("import java.util.HashMap;")
            appendLine("import java.util.Map;")
            appendLine()
            appendLine("public class $className {")
            appendLine("    private $className() {}")
            appendLine("    public static final Map<String, String> LIBS = new HashMap<>();")
            appendLine("    static {")
            versions.forEach { (libName, libJar) ->
                appendLine("        LIBS.put(\"$libName\", \"$libJar\");")
            }
            appendLine("    }")
            appendLine("}")
        }

        val outputDir = File(generatedSrcDir, packageName.replace('.', '/'))
        outputDir.mkdirs()
        File(outputDir, "$className.java").writeText(fileContent)
    }
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("generated/sources/data"))
}

tasks.named("processResources") {
    dependsOn(copyJarsToResources)
}
tasks.named("compileJava") {
    dependsOn(generateBundleMap)
}

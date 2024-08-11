package net.tinsvagelj.mc.modenv.gradle;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import net.tinsvagelj.mc.modenv.gradle.Data;

import java.io.*;
import java.util.*;

public class ModEnvGradlePlugin implements Plugin<Project> {
    ModEnvExt ext;

    private static final List<String> MODULE_EXPORTS = List.of(
        "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    );

    @Override
    public void apply(Project project) {
        ext = project.getExtensions().create("modEnv", ModEnvExt.class, project);

        project.getTasks().create("checkModEnvironment", CheckEnvironmentTask.class, task -> {
            RegularFile lib;
            RegularFile checker;
            try {
                lib = unpackBundledLib(project, "modenv-lib");
                checker = unpackBundledLib(project, "modenv-checker");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            task.setClasspath(project.files(lib, checker));
        });

        project.getTasks().named("compileJava", compileJava -> {
            compileJava.dependsOn("checkModEnvironment");
        });
        project.getTasks().named("compileJava", task -> {
            Project compiled = task.getProject();
            task.doFirst(compileJava -> {
                ConfigurationContainer configurations = compiled.getConfigurations();
                Configuration implConfig = configurations.getByName("implementation");
                Configuration compileConfig = configurations.getByName("compileOnly");
                Configuration annotationConfig = configurations.getByName("annotationProcessor");

                boolean hasProcessor = annotationConfig.getDependencies().stream().anyMatch(it -> it == ext.plugin);
                if (hasProcessor) {
                    CompileOptions javaOptions = ((JavaCompile) compileJava).getOptions();
                    javaOptions.getCompilerArgs().addAll(List.of(
                            "-Xplugin:ModEnvPlugin " + ext.current
                    ));
                    if (JavaVersion.current().isJava9Compatible()) {
                        javaOptions.setFork(true);
                        Objects.requireNonNull(javaOptions.getForkOptions().getJvmArgs()).addAll(MODULE_EXPORTS);
                    }
                }
            });
        });
    }

    private static final HashSet<RegularFile> unpackedThisRun = new HashSet<>();
    public static RegularFile unpackBundledLib(Project project, String name) throws IOException {
        String libFile = Data.LIBS.get(name);
        Provider<RegularFile> resolved = project.getLayout().getBuildDirectory().file("modEnvUnbundled/" + libFile);
        if (!resolved.isPresent()) {
            return null;
        }
        RegularFile result = resolved.get();
        if (unpackedThisRun.contains(result)) {
            return result;
        }
        try(InputStream stream = ModEnvGradlePlugin.class.getClassLoader().getResourceAsStream("libs/" + libFile)) {
            if (stream == null) {
                throw new IOException("ModEnv plugin is missing bundled Jar libraries");
            }

            File target = result.getAsFile();
            File targetDir = target.getParentFile();
            boolean createdParentDir = targetDir.exists() || targetDir.mkdirs();
            if (!createdParentDir) {
                throw new IOException("Unable to extract bundled Jar libraries");
            }
            try (OutputStream out = new FileOutputStream(target)) {
                out.write(stream.readAllBytes());
            } catch (FileNotFoundException ignored) {}
        }
        unpackedThisRun.add(result);
        return result;
    }
}

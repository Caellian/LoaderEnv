package net.tinsvagelj.mc.loaderenv.gradle;

import net.tinsvagelj.mc.loaderenv.gradle.task.CheckEnvironmentTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.*;

public class LoaderEnvGradlePlugin implements Plugin<Project> {
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
    public static final String PROP_MOD_ENV_ENABLED = "net.tinsvagelj.mc.loaderenv.enabled";
    public static final String PROP_MOD_ENV_PROCESSOR_ENABLED = "net.tinsvagelj.mc.loaderenv.enabled.annotation_processor";
    public static final String PROP_MOD_ENVIRONMENT = "net.tinsvagelj.mc.loaderenv.project_environment";

    public static final String VERSION_LIBRARY = "0.+";
    public static final String VERSION_PROCESSOR = "0.+";

    @Override
    public void apply(Project project) {
        project.getTasks().create("checkLoaderEnvironment", CheckEnvironmentTask.class);

        project.getDependencies().getExtensions().add(LoaderEnvironmentExtension.class, "loaderenv", new LoaderEnvironmentExtension(project, project.getDependencies()));

        project.getTasks().named("compileJava", task -> {
            task.dependsOn("checkLoaderEnvironment");
            task.doFirst(compileJava -> {
                Project compiledProject = compileJava.getProject();
                if (compiledProject.property(PROP_MOD_ENV_PROCESSOR_ENABLED) instanceof Boolean hasProcessor && hasProcessor) {
                    CompileOptions javaOptions = ((JavaCompile) compileJava).getOptions();
                    javaOptions.getCompilerArgs().add(
                            "-Xplugin:LoaderEnvPlugin " + compiledProject.property(PROP_MOD_ENVIRONMENT)
                    );
                    if (JavaVersion.current().isJava9Compatible()) {
                        javaOptions.setFork(true);
                        Objects.requireNonNull(javaOptions.getForkOptions().getJvmArgs()).addAll(MODULE_EXPORTS);
                    }
                }
            });
        });
    }
}

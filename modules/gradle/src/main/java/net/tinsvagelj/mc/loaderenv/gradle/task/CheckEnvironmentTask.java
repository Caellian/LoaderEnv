package net.tinsvagelj.mc.loaderenv.gradle.task;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import net.tinsvagelj.mc.loaderenv.gradle.LoaderEnvGradlePlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

public class CheckEnvironmentTask extends DefaultTask {
    @TaskAction
    public void runJar() {
        Project project = getProject();
        if (project.property(LoaderEnvGradlePlugin.PROP_MOD_ENV_ENABLED) instanceof Boolean enabled && enabled) {
            TaskProvider<JavaCompile> compileJavaTask = project.getTasks().named("compileJava", JavaCompile.class);
            FileCollection compileClasspath = compileJavaTask.get().getClasspath();

            GroovyClassLoader classLoader = new GroovyClassLoader(getClass().getClassLoader());
            compileClasspath.getFiles().forEach(file -> classLoader.addClasspath(file.getAbsolutePath()));
            GroovyShell shell = new GroovyShell(classLoader);
            String environment = (String) shell.evaluate("Loader.getAvailable().collect { it.toString() }.join(';')");
            project.setProperty(LoaderEnvGradlePlugin.PROP_MOD_ENVIRONMENT, environment);
        }
    }
}

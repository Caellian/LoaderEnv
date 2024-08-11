package net.tinsvagelj.mc.modenv.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.process.ExecResult;

import java.io.ByteArrayOutputStream;

public class CheckEnvironmentTask extends DefaultTask {
    private FileCollection classpath;

    @InputFiles
    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    @TaskAction
    public void runJar() {
        TaskProvider<JavaCompile> compileJavaTask = getProject().getTasks().named("compileJava", JavaCompile.class);
        FileCollection compileClasspath = compileJavaTask.get().getClasspath();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ExecResult result = getProject().javaexec(spec -> {
            spec.setClasspath(compileClasspath.plus(classpath));
            spec.getMainClass().set("net.tinsvagelj.mc.modenv.checker.Main");  // Replace with the actual main class of the JAR
            spec.setStandardOutput(outputStream);
        });
        result.assertNormalExitValue();

        ModEnvExt ext = getProject().getExtensions().getByType(ModEnvExt.class);
        ext.current = outputStream.toString().trim();
    }
}

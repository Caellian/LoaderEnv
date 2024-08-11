package net.tinsvagelj.mc.modenv.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.RegularFile;

import java.io.*;

import static net.tinsvagelj.mc.modenv.gradle.ModEnvGradlePlugin.unpackBundledLib;

public class ModEnvExt {
    private final Project project;

    public final Dependency lib;
    public final Dependency plugin;

    public String current = "";

    public ModEnvExt(Project project) throws IOException {
        this.project = project;
        this.lib = bundledDependency("modenv-lib");
        this.plugin = bundledDependency("modenv-processor");
    }

    private Dependency bundledDependency(String name) throws IOException {
        RegularFile file = unpackBundledLib(project, name);
        if (file == null) {
            return null;
        }
        Dependency result = project.getDependencies().create(project.files(file));
        return result;
    }
}

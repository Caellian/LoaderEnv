package net.tinsvagelj.mc.loaderenv.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import java.util.Map;

import static net.tinsvagelj.mc.loaderenv.gradle.LoaderEnvGradlePlugin.*;

public class LoaderEnvironmentExtension {
    private final Project project;
    private final DependencyHandler dependencies;

    public LoaderEnvironmentExtension(Project project, DependencyHandler dependencies) {
        this.project = project;
        this.dependencies = dependencies;
    }

    /**
     * Adds LoaderEnv library and annotation processor dependencies.
     */
    public void loaderenv() {
        loaderenv(true);
    }
    /**
     * Adds LoaderEnv library and annotation processor dependencies.
     * <p>
     * For a list of options see {@link LoaderEnvironmentExtension#loaderenv(boolean)}.
     */
    public void loaderenv(Map<String, Object> options) {
        boolean processor = (boolean) options.getOrDefault("processor", true);
        loaderenv(processor);
    }
    /**
     * Adds LoaderEnv library and annotation processor dependencies.
     *
     * @param processor controls use of annotation processor. Use <em>false</em> to prevent annotation processing.
     */
    public void loaderenv(boolean processor) {
        dependencies.add("implementation", "net.tinsvagelj.mc:loaderenv-lib:" + VERSION_LIBRARY);
        if (processor) {
            dependencies.add("annotationProcessor", "net.tinsvagelj.mc:loaderenv-lib:" + VERSION_LIBRARY);
            dependencies.add("annotationProcessor", "net.tinsvagelj.mc:loaderenv-processor:" + VERSION_PROCESSOR);
            project.setProperty(PROP_MOD_ENV_PROCESSOR_ENABLED, true);
        }
        project.setProperty(PROP_MOD_ENV_ENABLED, true);
    }
}

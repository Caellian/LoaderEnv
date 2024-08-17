package net.tinsvagelj.mc.loaderenv.intellij.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.messages.MessageBusConnection;
import net.tinsvagelj.mc.loaderenv.Loader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class LoaderEnvironmentService implements Disposable {
    Project project;
    Loader[] currentEnvironment;

    public LoaderEnvironmentService(Project project) {
        this.project = project;
        this.currentEnvironment = null;
        registerListeners();
    }

    private void registerListeners() {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
            @Override
            public void rootsChanged(@NotNull ModuleRootEvent event) {
                if (!event.isCausedByWorkspaceModelChangesOnly()) {
                    return;
                }
                currentEnvironment = detectLoaders();
            }
        });
    }

    public Loader[] getLoaders() {
        if (currentEnvironment == null) {
            currentEnvironment = detectLoaders();
        }
        return currentEnvironment;
    }

    private Loader[] detectLoaders() {
        List<Loader> result = new ArrayList<>(1);
        for (Loader env : Loader.values()) {
            for (String className : env.detectClasses) {
                if (findClassInScope(className) != null) {
                    result.add(env);
                    break;
                }
            }
        }
        return result.toArray(Loader[]::new);
    }
    private PsiClass findClassInScope(String canonicalName) {
        return JavaPsiFacade.getInstance(project).findClass(canonicalName, GlobalSearchScope.allScope(project));
    }

    @Override
    public void dispose() {}
}

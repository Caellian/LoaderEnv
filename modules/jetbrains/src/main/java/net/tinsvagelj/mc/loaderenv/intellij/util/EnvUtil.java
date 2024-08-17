package net.tinsvagelj.mc.loaderenv.intellij.util;

import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.concurrency.ThreadingAssertions;
import net.tinsvagelj.mc.loaderenv.Loader;
import org.jetbrains.annotations.Nullable;

public final class EnvUtil {
    private EnvUtil() {}

    public static boolean isLoaderEnvModule(@Nullable Module module) {
        return JavaLibraryUtil.hasLibraryClass(module, Loader.class.getCanonicalName());
    }

    private static boolean hasNestedChild(VirtualFile file, String... segments) {
        VirtualFile current = file;
        for (String segment : segments) {
            current = current.findChild(segment);
            if(current == null) {
                return false;
            }
        }
        return true;
    }
    // copied from Lombok plugin
    private static boolean locateLibraryPackageSlow(Project project) {
        // it is required for JARs attached directly from disk
        // or via build systems that do not supply Maven coordinates properly via LibraryWithMavenCoordinatesProperties
        return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
            Ref<Boolean> exists = new Ref<>(false);
            OrderEnumerator.orderEntries(project).recursively()
                    .forEachLibrary(library -> {
                        VirtualFile[] libraryFiles = library.getFiles(OrderRootType.CLASSES);
                        JarFileSystem jarFileSystem = JarFileSystem.getInstance();

                        for (VirtualFile libraryFile : libraryFiles) {
                            if (libraryFile.getFileSystem() != jarFileSystem) continue;

                            // look into every JAR for top level package entry
                            if (hasNestedChild(libraryFile, "net", "tinsvagelj", "mc", "loaderenv")) {
                                exists.set(true);
                                return false;
                            }
                        }

                        return true;
                    });

            return CachedValueProvider.Result.create(exists.get(), ProjectRootManager.getInstance(project));
        });
    }
    public static boolean noLoaderEnvLibrary(Project project) {
        if (project.isDefault() || !project.isInitialized()) {
            return true;
        }

        ThreadingAssertions.assertReadAccess();
        return !JavaLibraryUtil.hasLibraryJar(project, "net.tinsvagelj.mc:loaderenv-lib")
                && !locateLibraryPackageSlow(project);
    }
}

package net.tinsvagelj.mc.loaderenv.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import net.tinsvagelj.mc.loaderenv.Loader;
import net.tinsvagelj.mc.loaderenv.intellij.service.LoaderEnvironmentService;
import net.tinsvagelj.mc.loaderenv.intellij.highlight.DisabledElementTracker;
import net.tinsvagelj.mc.loaderenv.intellij.util.EnvUtil;
import net.tinsvagelj.mc.loaderenv.intellij.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

public class EnvironmentDisabledElementInspection extends AbstractBaseJavaLocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (EnvUtil.noLoaderEnvLibrary(holder.getProject())) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
        return new Visitor();
    }

    private static class Visitor extends PsiElementVisitor {
        DisabledElementTracker disabledElements = null;
        boolean dirty = false;

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (!PsiUtil.hasLoaderEnvAnnotation(element)) {
                super.visitElement(element);
                return;
            }

            Project project = element.getProject();
            LoaderEnvironmentService service = project.getService(LoaderEnvironmentService.class);

            DisabledElementTracker cache = DisabledElementTracker.getOrCreate(element.getContainingFile());
            if (checkElementAllowed(element, service.getLoaders())) {
                dirty |= cache.isDirectlyDisabled(element);
                cache.markEnabled(element);
                super.visitElement(element);
            } else {
                dirty |= !cache.isDirectlyDisabled(element);
                cache.markDisabled(element);
            }
        }

        @Override
        public void visitFile(@NotNull PsiFile file) {
            disabledElements = DisabledElementTracker.getOrCreate(file);
            super.visitFile(file);
            if (disabledElements.isEmpty()) {
                file.putUserData(DisabledElementTracker.DISABLED_ELEMENTS_KEY, null);
            }
            if (dirty) {
                // DaemonCodeAnalyzer.getInstance(file.getProject()).restart(file);
                dirty = false;
            }
            disabledElements = null;
        }
    }

    public static <E extends PsiElement> boolean checkElementAllowed(E element, Loader[] projectEnvironment) {
        if (projectEnvironment.length == 0) return true;
        Loader[] allowed = PsiUtil.getElementAllowedLoaders(element);
        for (Loader a : allowed) {
            for (Loader b : projectEnvironment) {
                if (a == b) {
                    return true;
                }
            }
        }
        return false;
    }
}

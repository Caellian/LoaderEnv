package net.tinsvagelj.mc.loaderenv.intellij.highlight;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import net.tinsvagelj.mc.loaderenv.intellij.util.EnvUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnvironmentStateErrorFilter implements HighlightInfoFilter {
    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (file == null) {
            return true;
        }

        Project project = file.getProject();
        if (EnvUtil.noLoaderEnvLibrary(project)) {
            return true;
        }

        DisabledElementTracker disabledElements = file.getUserData(DisabledElementTracker.DISABLED_ELEMENTS_KEY);
        if (disabledElements == null) return true;

        HighlightDetail detail = HighlightDetail.parse(project, file, highlightInfo);
        if (detail != null) {
            for (PsiElement other : detail.conflicts()) {
                if (!disabledElements.isDisabled(other)) {
                    return true;
                }
            }
        }

        return false;
    }
}

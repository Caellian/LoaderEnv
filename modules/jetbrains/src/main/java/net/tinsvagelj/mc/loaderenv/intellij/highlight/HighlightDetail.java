package net.tinsvagelj.mc.loaderenv.intellij.highlight;

import com.intellij.codeInsight.daemon.JavaErrorBundle;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import net.tinsvagelj.mc.loaderenv.intellij.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface HighlightDetail {
    PsiElement[] conflicts();

    record DuplicateDefinition<T extends PsiElement>(T current, T[] other) implements HighlightDetail {
        @Override
        public PsiElement[] conflicts() {
            return this.other;
        }
    }

    record InvalidOverride<T extends PsiElement>(T current, T[] other) implements HighlightDetail {
        @Override
        public PsiElement[] conflicts() {
            return this.other;
        }
    }

    static @Nullable HighlightDetail parse(@NotNull Project project, @NotNull PsiFile file, @NotNull HighlightInfo info) {
        @Nullable PsiElement element = file.findElementAt(info.getStartOffset());
        if (element == null) return null;
        @Nullable PsiClass owner = PsiUtil.findClassInFileLocation(file, info.getStartOffset());

        final String description = StringUtil.notNullize(info.getDescription());
        {
            String className = CachedArgumentMatcher.DUPLICATE_CLASS_MSG.getArg(description, 0);
            if (className != null && owner != null) {
                PsiClass self = (PsiClass) element.getParent();
                PsiClass[] duplicates = PsiUtil.findSameNameSiblings(self).toArray(PsiClass[]::new);
                return new DuplicateDefinition<>(owner, duplicates);
            }
        }
        {
            String[] args = CachedArgumentMatcher.DUPLICATE_METHOD_MSG.getArgs(description);
            if (args != null) {
                PsiClass parent = JavaPsiFacade.getInstance(project).findClass(args[1], GlobalSearchScope.allScope(project));
                PsiMethod[] duplicates = PsiUtil.findMethodsInClass(parent, args[0])
                        .filter(it -> it != element).toArray(PsiMethod[]::new);
                return new DuplicateDefinition<>((PsiMethod) element, duplicates);
            }
        }
        {
            String varName = CachedArgumentMatcher.DUPLICATE_VARIABLE_MSG.getArg(description, 0);
            if (varName != null) {
                if (element.getParent() instanceof PsiField field) {
                    PsiVariable[] variables = PsiUtil.findPreviousVariables(field).toArray(PsiVariable[]::new);
                    return new DuplicateDefinition<>(field, variables);
                }
            }
        }
        {
            String[] args = CachedArgumentMatcher.OVERRIDE_FINAL.getArgs(description);
            if (args != null) {
                PsiClass parent = JavaPsiFacade.getInstance(project).findClass(args[2], GlobalSearchScope.allScope(project));
                PsiMethod[] previousFinal = PsiUtil.findMethodsInClass(parent, args[1]).toArray(PsiMethod[]::new);
                return new InvalidOverride<>((PsiMethod) element, previousFinal);
            }
        }
        {
            String[] args = CachedArgumentMatcher.INSTANCE_OVERRIDE_STATIC.getArgs(description);
            if (args != null) {
                PsiClass parent = JavaPsiFacade.getInstance(project).findClass(args[3], GlobalSearchScope.allScope(project));
                PsiMethod[] previousFinal = PsiUtil.findMethodsInClass(parent, args[2]).toArray(PsiMethod[]::new);
                return new InvalidOverride<>((PsiMethod) element, previousFinal);
            }
        }
        {
            String[] args = CachedArgumentMatcher.STATIC_OVERRIDE_INSTANCE.getArgs(description);
            if (args != null) {
                PsiClass parent = JavaPsiFacade.getInstance(project).findClass(args[3], GlobalSearchScope.allScope(project));
                PsiMethod[] previousFinal = PsiUtil.findMethodsInClass(parent, args[2]).toArray(PsiMethod[]::new);
                return new InvalidOverride<>((PsiMethod) element, previousFinal);
            }
        }

        return null;
    }


    // Sadly HighlightInfo is untyped, so we HAVE to pattern match error messages.
    class CachedArgumentMatcher {
        private static final CachedArgumentMatcher DUPLICATE_CLASS_MSG = new CachedArgumentMatcher("duplicate.class", 1);
        private static final CachedArgumentMatcher DUPLICATE_METHOD_MSG = new CachedArgumentMatcher("duplicate.method", 2);
        private static final CachedArgumentMatcher DUPLICATE_VARIABLE_MSG = new CachedArgumentMatcher("variable.already.defined", 1);

        private static final CachedArgumentMatcher OVERRIDE_FINAL = new CachedArgumentMatcher("final.method.override", 3);
        private static final CachedArgumentMatcher INSTANCE_OVERRIDE_STATIC = new CachedArgumentMatcher("instance.method.cannot.override.static.method", 4);
        private static final CachedArgumentMatcher STATIC_OVERRIDE_INSTANCE = new CachedArgumentMatcher("static.method.cannot.override.instance.method", 4);

        Pattern matchPattern;
        int argc;

        private CachedArgumentMatcher(String key, int argc) {
            Object[] args = new Object[argc];
            Arrays.fill(args, "([^']+)");
            this.matchPattern = Pattern.compile(JavaErrorBundle.message(key, args));
        }

        private CachedArgumentMatcher(String key, Object... args) {
            this.matchPattern = Pattern.compile(JavaErrorBundle.message(key, args));
        }

        private @Nullable String getArg(String term, int argi) {
            Matcher m = this.matchPattern.matcher(term);
            if (!m.matches()) return null;
            return m.group(argi + 1);
        }

        private @Nullable String[] getArgs(String term) {
            Matcher m = this.matchPattern.matcher(term);
            if (!m.matches()) return null;
            String[] result = new String[this.argc];
            for (int i = 0; i < this.argc; i++) {
                result[i] = m.group(i + 1);
            }
            return result;
        }
    }
}

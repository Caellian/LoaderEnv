package net.tinsvagelj.mc.loaderenv.intellij.util;

import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.scope.PatternResolveState;
import com.intellij.psi.scope.processor.VariablesNotProcessor;
import com.intellij.psi.scope.processor.VariablesProcessor;
import com.intellij.psi.scope.util.PsiScopesUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import net.tinsvagelj.mc.loaderenv.Loader;
import net.tinsvagelj.mc.loaderenv.NotForLoader;
import net.tinsvagelj.mc.loaderenv.OnlyForLoader;
import org.assertj.core.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.*;

public final class PsiUtil {
    public static final String[] LOADER_FILTER_ANNOTATIONS = {
        NotForLoader.class.getCanonicalName(),
        OnlyForLoader.class.getCanonicalName()
    };

    private PsiUtil() {}

    public static PsiClass findClassInFileLocation(PsiFile psiFile, int offset) {
        PsiClass[] fileClasses = PsiTreeUtil.getChildrenOfType(psiFile, PsiClass.class);
        if (fileClasses == null) return null;
        for (PsiClass fileClass : fileClasses) {
            if (fileClass.getTextRange().contains(offset)) {
                return fileClass;
            }
        }
        return null;
    }

    public static Stream<PsiElement> findSiblings(@NotNull PsiElement of) {
        return Arrays.stream(of.getParent().getChildren()).filter(it -> !it.equals(of));
    }

    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> Stream<T> findSameSiblings(@NotNull T of) {
        Class<T> targetClass = (Class<T>) of.getClass();
        return findSiblings(of)
                .filter(it -> targetClass.isAssignableFrom(it.getClass()))
                .map(it -> (T) it);
    }

    @SuppressWarnings("unchecked")
    public static <T extends PsiNamedElement> Stream<T> findSameNameSiblings(@NotNull T named) {
        if (named instanceof PsiQualifiedNamedElement qual) {
            return (Stream<T>) findSameSiblings(qual)
                    .filter(it -> Objects.equals(it.getQualifiedName(), qual.getQualifiedName()));
        }
        return findSameSiblings(named)
                .filter(it -> Objects.equals(it.getName(), named.getName()));
    }

    public static Stream<PsiMethod> findMethodsInClass(PsiClass clazz, String methodSignature) {
        if (clazz == null || methodSignature == null) {
            return Stream.empty();
        }

        PsiElementFactory elementFactory = PsiElementFactory.getInstance(clazz.getProject());
        UntypedMethodSignature signature = new UntypedMethodSignature(methodSignature, elementFactory);

        return Arrays.stream(clazz.getMethods()).filter(signature::matches);
    }

    @SuppressWarnings("unchecked")
    private static <T extends PsiVariable> Stream<T> variablesNotStream(VariablesProcessor proc) {
        ArrayList<T> results = new ArrayList<>(proc.size());
        for (int i = 0; i < proc.size(); i++) {
            results.add((T) proc.getResult(i));
        }
        return results.stream();
    }

    // Adapted from com.intellij.codeInsight.daemon.impl.analysis.HighlightUtil
    // Changes:
    // - Return actual value instead of HighlightInfo
    // - Return multiple values
    public static @NotNull Stream<? extends PsiVariable> findPreviousVariables(@NotNull PsiVariable variable) {
        if (variable instanceof ExternallyDefinedPsiElement || variable.isUnnamed()) return null;
        Stream<? extends PsiVariable> prevDeclarations = Stream.empty();
        PsiElement declarationScope = null;
        if (variable instanceof PsiLocalVariable || variable instanceof PsiPatternVariable ||
                variable instanceof PsiParameter &&
                        ((declarationScope = ((PsiParameter)variable).getDeclarationScope()) instanceof PsiCatchSection ||
                                declarationScope instanceof PsiForeachStatement ||
                                declarationScope instanceof PsiLambdaExpression)) {
            PsiElement currentScope =
                    PsiTreeUtil.getParentOfType(variable, PsiFile.class, PsiMethod.class, PsiClassInitializer.class, PsiResourceList.class);
            VariablesNotProcessor proc = new VariablesNotProcessor(variable, false) {
                @Override
                protected boolean check(PsiVariable var, ResolveState state) {
                    return com.intellij.psi.util.PsiUtil.isJvmLocalVariable(var) && super.check(var, state);
                }
            };
            PsiIdentifier identifier = variable.getNameIdentifier();
            assert identifier != null : variable;
            PsiScopesUtil.treeWalkUp(proc, identifier, currentScope);
            if (currentScope instanceof PsiResourceList && proc.size() == 0) {
                currentScope = PsiTreeUtil.getParentOfType(variable, PsiFile.class, PsiMethod.class, PsiClassInitializer.class);
                PsiScopesUtil.treeWalkUp(proc, identifier, currentScope);
            }
            if (proc.size() > 0) {
                prevDeclarations = variablesNotStream(proc);
            } else if (declarationScope instanceof PsiLambdaExpression) {
                prevDeclarations = findSameNameSiblings(variable);
            } else if (variable instanceof PsiPatternVariable) {
                prevDeclarations = findSamePatternVariableInBranches((PsiPatternVariable) variable);
            }
        } else if (variable instanceof PsiField field) {
            PsiClass parentClass = field.getContainingClass();
            if (parentClass == null) return null;
            prevDeclarations = Stream.concat(
                Arrays.stream(parentClass.getFields()).filter(it -> !it.equals(variable) && it.getName().equals(variable.getName())),
                Arrays.stream(parentClass.getRecordComponents()).filter(it -> field.getName().equals(it.getName()))
            );
        } else {
            prevDeclarations = findSameNameSiblings(variable);
        }

        return prevDeclarations;
    }

    private static Stream<PsiPatternVariable> findSamePatternVariableInBranches(@NotNull PsiPatternVariable variable) {
        PsiPattern pattern = variable.getPattern();
        PatternResolveState hint = PatternResolveState.WHEN_TRUE;
        VariablesNotProcessor proc = new VariablesNotProcessor(variable, false) {
            @Override
            protected boolean check(PsiVariable var, ResolveState state) {
                return var instanceof PsiPatternVariable && super.check(var, state);
            }
        };
        PsiElement lastParent = pattern;
        for (PsiElement parent = lastParent.getParent(); parent != null; lastParent = parent, parent = parent.getParent()) {
            if (parent instanceof PsiInstanceOfExpression || parent instanceof PsiParenthesizedExpression) continue;
            if (parent instanceof PsiPrefixExpression && ((PsiPrefixExpression)parent).getOperationTokenType().equals(JavaTokenType.EXCL)) {
                hint = hint.invert();
                continue;
            }
            if (parent instanceof PsiPolyadicExpression) {
                IElementType tokenType = ((PsiPolyadicExpression)parent).getOperationTokenType();
                if (tokenType.equals(JavaTokenType.ANDAND) || tokenType.equals(JavaTokenType.OROR)) {
                    PatternResolveState targetHint = PatternResolveState.fromBoolean(tokenType.equals(JavaTokenType.OROR));
                    if (hint == targetHint) {
                        for (PsiExpression operand : ((PsiPolyadicExpression)parent).getOperands()) {
                            if (operand == lastParent) break;
                            operand.processDeclarations(proc, hint.putInto(ResolveState.initial()), null, pattern);
                        }
                    }
                    continue;
                }
            }
            if (parent instanceof PsiConditionalExpression conditional) {
                PsiExpression thenExpression = conditional.getThenExpression();
                if (lastParent == thenExpression) {
                    conditional.getCondition()
                            .processDeclarations(proc, PatternResolveState.WHEN_FALSE.putInto(ResolveState.initial()), null, pattern);
                }
                else if (lastParent == conditional.getElseExpression()) {
                    conditional.getCondition()
                            .processDeclarations(proc, PatternResolveState.WHEN_TRUE.putInto(ResolveState.initial()), null, pattern);
                    if (thenExpression != null) {
                        thenExpression.processDeclarations(proc, hint.putInto(ResolveState.initial()), null, pattern);
                    }
                }
            }
            break;
        }
        return variablesNotStream(proc);
    }

    public static boolean isAnnotationClasspath(PsiAnnotation annotation, String... oneOf) {
        String name = annotation.getQualifiedName();
        return Arrays.asList(oneOf).contains(name);
    }

    static <T extends JvmAnnotationAttributeValue> @Nullable Loader parseLoaderEntry(T attributeValue) {
        if (attributeValue instanceof JvmAnnotationEnumFieldValue enumValue) {
            String clazz = enumValue.getContainingClassName();
            if (clazz == null || !clazz.endsWith("Loader")) return null;
            String name = enumValue.getFieldName();
            return name != null ? Loader.parse(name) : null;
        }
        return null;
    }

    public static Loader[] parseLoaderEntries(PsiAnnotation annotation) {
        if (!isAnnotationClasspath(annotation, LOADER_FILTER_ANNOTATIONS)) {
            return new Loader[]{};
        }
        List<JvmAnnotationAttribute> attributes = annotation.getAttributes();
        if (attributes.isEmpty()) {
            return new Loader[]{};
        }
        if (attributes.size() == 1) {
            JvmAnnotationAttribute attribute = attributes.getFirst();
            @Nullable JvmAnnotationAttributeValue value = attribute.getAttributeValue();
            if (value instanceof JvmAnnotationArrayValue loaderArrayArg) {
                return loaderArrayArg.getValues()
                        .stream()
                        .map(PsiUtil::parseLoaderEntry)
                        .filter(Objects::nonNull)
                        .toArray(Loader[]::new);
            }
        }
        // Not handled.
        return new Loader[]{};
    }

    private static Loader[] resolveAllowedLoaders(@Nullable PsiAnnotation onlyFor, @Nullable PsiAnnotation notFor) {
        List<Loader> result = onlyFor != null ? Lists.newArrayList(parseLoaderEntries(onlyFor)) : Lists.newArrayList(Loader.values());
        if (notFor != null) {
            result.removeAll(List.of(parseLoaderEntries(notFor)));
        }
        return result.toArray(Loader[]::new);
    }

    public static Loader[] getElementAllowedLoaders(PsiElement element) {
        if (element instanceof PsiJvmModifiersOwner annotationOwner) {
            Optional<PsiAnnotation> onlyFor = Arrays.stream(annotationOwner.getAnnotations()).filter(it -> Objects.equals(it.getQualifiedName(), OnlyForLoader.class.getCanonicalName())).findFirst();
            Optional<PsiAnnotation> notFor = Arrays.stream(annotationOwner.getAnnotations()).filter(it -> Objects.equals(it.getQualifiedName(), NotForLoader.class.getCanonicalName())).findFirst();
            return resolveAllowedLoaders(onlyFor.orElse(null), notFor.orElse(null));
        }
        return Loader.values();
    }

    public static <E extends PsiElement> PsiAnnotation[] getLoaderEnvAnnotations(E element) {
        ArrayList<PsiAnnotation> annotations = new ArrayList<>();
        if (element instanceof PsiJvmModifiersOwner annotationOwner) {
            for (PsiAnnotation annotation : annotationOwner.getAnnotations()) {
                if (isAnnotationClasspath(annotation, LOADER_FILTER_ANNOTATIONS)) {
                    annotations.add(annotation);
                }
            }
        }
        return annotations.toArray(PsiAnnotation[]::new);
    }

    public static <E extends PsiElement> boolean hasLoaderEnvAnnotation(E element) {
        if (element instanceof PsiJvmModifiersOwner annotationOwner) {
            for (PsiAnnotation annotation : annotationOwner.getAnnotations()) {
                if (isAnnotationClasspath(annotation, LOADER_FILTER_ANNOTATIONS)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class UntypedMethodSignature {
        final String name;
        final MybResolved[] argumentTypes;

        PsiMethod resolved = null;

        public UntypedMethodSignature(String signature) {
            int brace = signature.indexOf('(');
            if (brace == -1) {
                this.name = signature;
                this.argumentTypes = new MybResolved[]{};
                return;
            }
            this.name = signature.substring(0, brace);

            String argSegment = signature.substring(brace+1, signature.length()-2);
            this.argumentTypes = Arrays.stream(argSegment.split(","))
                    .map(MybResolved::new)
                    .toArray(MybResolved[]::new);
        }
        public UntypedMethodSignature(String signature, PsiElementFactory elementFactory) {
            this(signature);
            this.resolveArgumentTypes(elementFactory);
        }
        public void resolveArgumentTypes(PsiElementFactory elementFactory) {
            for (MybResolved argument : this.argumentTypes) {
                argument.resolve(elementFactory, null);
            }
        }

        public boolean matches(PsiMethod method) {
            if (!method.getName().equals(this.name)) return false;
            PsiParameter[] params = method.getParameterList().getParameters();
            if (params.length != this.argumentTypes.length) return false;
            for (int i = 0; i < params.length; i++) {
                if (!this.argumentTypes[i].matches(params[i].getType())) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class MybResolved {
        String unresolved;
        PsiType resolved = null;

        public MybResolved(String unresolved) {
            this.unresolved = unresolved.trim();
        }
        public MybResolved(PsiType resolved) {
            this.unresolved = resolved.getCanonicalText();
            this.resolved = resolved;
        }

        public @Nullable PsiType getResolved() {
            return this.resolved;
        }
        public boolean isResolved() {
            return this.resolved != null;
        }
        public String getName() {
            return this.unresolved;
        }

        public void resolve(PsiElementFactory elementFactory, @Nullable PsiElement text) {
            elementFactory.createTypeFromText(this.unresolved, text);
        }

        public boolean matches(PsiType type) {
            if (!this.isResolved()) return type.equalsToText(this.unresolved);
            return this.resolved.equals(type);
        }
    }
}

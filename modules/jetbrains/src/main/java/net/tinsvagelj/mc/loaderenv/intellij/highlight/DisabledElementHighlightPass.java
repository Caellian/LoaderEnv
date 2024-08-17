package net.tinsvagelj.mc.loaderenv.intellij.highlight;

import com.intellij.codeHighlighting.*;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import net.tinsvagelj.mc.loaderenv.intellij.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DisabledElementHighlightPass extends TextEditorHighlightingPass {
    public static final Key<ArrayList<DisabledElementHighlightGroup>> DISABLED_ELEMENT_HIGHLIGHT_GROUPS_KEY = new Key<>("net.tinsvagelj.mc.loaderenv.disabled_element_highlighters");

    private final PsiFile file;
    private final Editor editor;

    protected DisabledElementHighlightPass(Project project, Document document, PsiFile file, Editor editor) {
        super(project, document, true);
        this.file = file;
        this.editor = editor;
    }

    @Override
    public void doCollectInformation(@NotNull ProgressIndicator progress) {}

    @Override
    public void doApplyInformationToEditor() {
        MarkupModel markupModel = editor.getMarkupModel();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                DisabledElementTracker disabled = file.getUserData(DisabledElementTracker.DISABLED_ELEMENTS_KEY);
                ArrayList<DisabledElementHighlightGroup> highlighters = file.getUserData(DISABLED_ELEMENT_HIGHLIGHT_GROUPS_KEY);
                ArrayList<DisabledElementHighlightGroup> kept = new ArrayList<>(highlighters != null ? highlighters.size() : 0);

                if (highlighters != null) {
                    for (DisabledElementHighlightGroup h : highlighters) {
                        if (disabled == null || !disabled.isDisabled(h.element)) {
                            h.removeHighlighters(markupModel);
                        } else {
                            kept.add(h);
                        }
                    }
                }

                if (disabled != null) {
                    for (PsiElement el : disabled.getDisabled()) {
                        if (kept.stream().anyMatch(it -> it.element == el)) continue;

                        PsiAnnotation[] annotations = PsiUtil.getLoaderEnvAnnotations(el);
                        List<Integer> offsets = cutOutAnnotations(el, annotations);

                        int halfSize = offsets.size() / 2;
                        RangeHighlighter[] highlighter = new RangeHighlighter[offsets.size() - 1];
                        for (int i = 0; i < halfSize; i++) {
                            highlighter[i] = markupModel.addRangeHighlighter(
                                    DefaultLanguageHighlighterColors.BLOCK_COMMENT,
                                    offsets.get(i * 2),
                                    offsets.get(i * 2 + 1),
                                    HighlighterLayer.CONSOLE_FILTER,
                                    HighlighterTargetArea.EXACT_RANGE
                            );
                            if (i < halfSize - 1) {
                                highlighter[halfSize + i] = markupModel.addRangeHighlighter(
                                        DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR,
                                        offsets.get(i * 2 + 1),
                                        offsets.get(i * 2 + 2),
                                        HighlighterLayer.CONSOLE_FILTER,
                                        HighlighterTargetArea.EXACT_RANGE
                                );
                            }
                        }
                        kept.add(new DisabledElementHighlightGroup(el, highlighter));
                    }
                }

                file.putUserData(DISABLED_ELEMENT_HIGHLIGHT_GROUPS_KEY, kept.isEmpty() ? null : kept);
            }
        });
    }

    private static @NotNull List<Integer> cutOutAnnotations(PsiElement el, PsiAnnotation[] annotations) {
        ArrayList<Integer> offsets = new ArrayList<>();
        TextRange range = el.getTextRange();
        offsets.add(range.getStartOffset());
        offsets.add(range.getEndOffset());
        for (PsiAnnotation annotationInner : annotations) {
            TextRange aRange = annotationInner.getTextRange();
            offsets.add(aRange.getStartOffset());
            offsets.add(aRange.getEndOffset());
        }
        offsets.sort(null);
        return offsets;
    }

    public static class DisabledElementHighlightGroup {
        PsiElement element;
        RangeHighlighter[] inner;

        public DisabledElementHighlightGroup(PsiElement element, RangeHighlighter[] inner) {
            this.element = element;
            this.inner = inner;
        }

        public void removeHighlighters(MarkupModel model) {
            for (RangeHighlighter highlighter : this.inner) {
                model.removeHighlighter(highlighter);
            }
        }
    }

    public static class Factory implements TextEditorHighlightingPassFactory {
        @Override
        public @Nullable TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor) {
            if (!file.getFileType().getDefaultExtension().equals(JavaFileType.DEFAULT_EXTENSION)) return null;

            Project project = file.getProject();
            Document document = editor.getDocument();
            return new DisabledElementHighlightPass(project, document, file, editor);
        }
    }

    public static class FactoryRegistrar implements TextEditorHighlightingPassFactoryRegistrar {
        @Override
        public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
            registrar.registerTextEditorHighlightingPass(new Factory(), TextEditorHighlightingPassRegistrar.Anchor.LAST, Pass.LOCAL_INSPECTIONS, false, false);
        }
    }
}

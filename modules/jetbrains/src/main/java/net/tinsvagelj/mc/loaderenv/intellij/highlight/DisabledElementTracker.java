package net.tinsvagelj.mc.loaderenv.intellij.highlight;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisabledElementTracker {
    public static final Key<DisabledElementTracker> DISABLED_ELEMENTS_KEY = new Key<>("net.tinsvagelj.mc.loaderenv.disabled_elements");

    List<PsiElement> elements;

    public DisabledElementTracker() {
        elements = new ArrayList<>();
    }

    public <E extends PsiElement> void markEnabled(E element) {
        this.elements.remove(element);
    }
    public <E extends PsiElement> void markDisabled(E element) {
        this.elements.add(element);
    }
    public <E extends PsiElement> boolean isDirectlyDisabled(E element) {
        return this.elements.contains(element);
    }
    public <E extends PsiElement> boolean isDisabled(E element) {
        return this.isDirectlyDisabled(element) || element != null && checkRangeDisabled(element.getTextRange());
    }
    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

    public List<PsiElement> getDisabled() {
        return Collections.unmodifiableList(this.elements);
    }

    public boolean checkRangeDisabled(int start, int end) {
        for (PsiElement el : this.elements) {
            if (el.getTextRange().containsRange(start, end)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkRangeDisabled(TextRange range) {
        for (PsiElement el : this.elements) {
            if (el.getTextRange().contains(range)) {
                return true;
            }
        }
        return false;
    }

    public static DisabledElementTracker getOrCreate(PsiFile file) {
        DisabledElementTracker result = file.getUserData(DISABLED_ELEMENTS_KEY);
        if (result == null) {
            result = new DisabledElementTracker();
            file.putUserData(DISABLED_ELEMENTS_KEY, result);
        }
        return result;
    }
}

package com.personal.brunohelper.context;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.personal.brunohelper.util.SpringControllerUtil;
import org.jetbrains.annotations.Nullable;

public final class ControllerContextResolver {

    private ControllerContextResolver() {
    }

    public static @Nullable PsiClass resolveController(AnActionEvent event) {
        PsiElement element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element == null && event.getData(CommonDataKeys.EDITOR) != null) {
            PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
            if (psiFile != null) {
                element = PsiUtilBase.getElementAtCaret(event.getData(CommonDataKeys.EDITOR));
            }
        }
        if (element == null) {
            return null;
        }

        PsiClass psiClass = element instanceof PsiClass ? (PsiClass) element : PsiTreeUtil.getParentOfType(element, PsiClass.class);
        return SpringControllerUtil.isSpringController(psiClass) ? psiClass : null;
    }
}

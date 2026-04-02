package com.personal.brunohelper.parser;

import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DocCommentUtil {

    private DocCommentUtil() {
    }

    public static String extractDescription(PsiDocCommentOwner owner) {
        PsiDocComment comment = owner.getDocComment();
        if (comment == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (var element : comment.getDescriptionElements()) {
            builder.append(element.getText());
        }
        return normalizeWhitespace(builder.toString());
    }

    public static Map<String, String> extractParameterDescriptions(PsiDocCommentOwner owner) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        PsiDocComment comment = owner.getDocComment();
        if (comment == null) {
            return descriptions;
        }
        for (PsiDocTag tag : comment.getTags()) {
            if (!"param".equals(tag.getName()) || tag.getValueElement() == null) {
                continue;
            }
            String text = tag.getText();
            String paramName = tag.getValueElement().getText();
            int paramNameIndex = text.indexOf(paramName);
            if (paramNameIndex < 0) {
                continue;
            }
            String description = normalizeWhitespace(text.substring(paramNameIndex + paramName.length()));
            descriptions.put(paramName, description);
        }
        return descriptions;
    }

    public static String firstSentence(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int lineBreakIndex = text.indexOf('\n');
        if (lineBreakIndex >= 0) {
            return text.substring(0, lineBreakIndex).trim();
        }
        return text.trim();
    }

    private static String normalizeWhitespace(String text) {
        return text.replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}

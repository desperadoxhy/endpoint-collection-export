package com.personal.brunohelper.parser;

import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import org.jetbrains.annotations.Nullable;

import com.intellij.psi.PsiElement;
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
        for (PsiElement element : comment.getDescriptionElements()) {
            builder.append(element.getText());
        }
        return normalizeWhitespace(builder.toString());
    }

    public static String extractSummary(PsiDocCommentOwner owner) {
        PsiDocComment comment = owner.getDocComment();
        if (comment == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (PsiElement element : comment.getDescriptionElements()) {
            builder.append(element.getText());
        }
        return sanitizeSummaryText(builder.toString());
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
        String sanitized = sanitizeSummaryText(text);
        if (!sanitized.isBlank()) {
            return sanitized;
        }
        return text.trim();
    }

    public static String sanitizeSummaryText(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("(?i)<p\\b[^>]*>", "")
                .replaceAll("(?i)</li\\s*>", "\n")
                .replaceAll("(?i)<li\\b[^>]*>", "- ")
                .replaceAll("<[^>]+>", " ");
        for (String line : normalized.split("\n")) {
            String sanitizedLine = normalizeWhitespace(line);
            if (!sanitizedLine.isBlank()) {
                return sanitizedLine;
            }
        }
        return normalizeWhitespace(normalized);
    }

    private static String normalizeWhitespace(String text) {
        return text.replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}

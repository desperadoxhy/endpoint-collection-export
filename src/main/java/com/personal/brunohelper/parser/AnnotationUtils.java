package com.personal.brunohelper.parser;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static @Nullable PsiAnnotation findAnnotation(PsiModifierListOwner owner, String... qualifiedNames) {
        if (owner.getModifierList() == null) {
            return null;
        }
        for (String qualifiedName : qualifiedNames) {
            PsiAnnotation annotation = owner.getModifierList().findAnnotation(qualifiedName);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    public static @Nullable PsiAnnotation findMethodAnnotationIncludingSupers(PsiMethod method, String... qualifiedNames) {
        PsiAnnotation annotation = findAnnotation(method, qualifiedNames);
        if (annotation != null) {
            return annotation;
        }
        for (PsiMethod superMethod : method.findSuperMethods()) {
            annotation = findAnnotation(superMethod, qualifiedNames);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    public static @Nullable PsiAnnotation findParameterAnnotationIncludingSupers(
            PsiMethod method,
            PsiParameter parameter,
            String... qualifiedNames
    ) {
        PsiAnnotation annotation = findAnnotation(parameter, qualifiedNames);
        if (annotation != null) {
            return annotation;
        }
        int index = method.getParameterList().getParameterIndex(parameter);
        if (index < 0) {
            return null;
        }
        for (PsiMethod superMethod : method.findSuperMethods()) {
            PsiParameter[] superParameters = superMethod.getParameterList().getParameters();
            if (index >= superParameters.length) {
                continue;
            }
            annotation = findAnnotation(superParameters[index], qualifiedNames);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    public static List<String> getStringArrayAttribute(PsiAnnotation annotation, String... attributeNames) {
        for (String attributeName : attributeNames) {
            PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
            List<String> values = toStringValues(value);
            if (!values.isEmpty()) {
                return values;
            }
        }
        return List.of();
    }

    public static @Nullable String getStringAttribute(PsiAnnotation annotation, String... attributeNames) {
        for (String attributeName : attributeNames) {
            PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
            String stringValue = toStringValue(value);
            if (stringValue != null && !stringValue.isBlank()) {
                return stringValue;
            }
        }
        return null;
    }

    public static boolean getBooleanAttribute(PsiAnnotation annotation, String attributeName, boolean defaultValue) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof PsiLiteralExpression literalExpression) {
            Object literalValue = literalExpression.getValue();
            if (literalValue instanceof Boolean booleanValue) {
                return booleanValue;
            }
        }
        return defaultValue;
    }

    public static List<String> getEnumArrayAttribute(PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (value == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        if (value instanceof PsiArrayInitializerMemberValue arrayValue) {
            for (PsiAnnotationMemberValue initializer : arrayValue.getInitializers()) {
                String enumName = toEnumName(initializer);
                if (enumName != null) {
                    values.add(enumName);
                }
            }
            return values;
        }
        String enumName = toEnumName(value);
        return enumName == null ? List.of() : List.of(enumName);
    }

    public static @Nullable String getValidationMessage(PsiModifierListOwner owner) {
        String[] annotationNames = {
                "javax.validation.constraints.NotNull",
                "javax.validation.constraints.NotBlank",
                "javax.validation.constraints.NotEmpty",
                "jakarta.validation.constraints.NotNull",
                "jakarta.validation.constraints.NotBlank",
                "jakarta.validation.constraints.NotEmpty"
        };
        PsiAnnotation annotation = findAnnotation(owner, annotationNames);
        if (annotation == null) {
            return null;
        }
        return getStringAttribute(annotation, "message");
    }

    public static boolean hasRequiredValidation(PsiModifierListOwner owner) {
        return findAnnotation(owner,
                "javax.validation.constraints.NotNull",
                "javax.validation.constraints.NotBlank",
                "javax.validation.constraints.NotEmpty",
                "jakarta.validation.constraints.NotNull",
                "jakarta.validation.constraints.NotBlank",
                "jakarta.validation.constraints.NotEmpty") != null;
    }

    private static List<String> toStringValues(@Nullable PsiAnnotationMemberValue value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof PsiArrayInitializerMemberValue arrayValue) {
            List<String> values = new ArrayList<>();
            for (PsiAnnotationMemberValue initializer : arrayValue.getInitializers()) {
                String stringValue = toStringValue(initializer);
                if (stringValue != null && !stringValue.isBlank()) {
                    values.add(stringValue);
                }
            }
            return values;
        }
        String stringValue = toStringValue(value);
        return stringValue == null || stringValue.isBlank() ? List.of() : List.of(stringValue);
    }

    private static @Nullable String toStringValue(@Nullable PsiAnnotationMemberValue value) {
        if (value instanceof PsiLiteralExpression literalExpression) {
            Object literalValue = literalExpression.getValue();
            return literalValue instanceof String ? (String) literalValue : null;
        }
        if (value != null) {
            return trimQuotes(value.getText());
        }
        return null;
    }

    private static @Nullable String toEnumName(PsiAnnotationMemberValue value) {
        if (value instanceof PsiReferenceExpression referenceExpression) {
            PsiElement resolved = referenceExpression.resolve();
            if (resolved instanceof PsiEnumConstant enumConstant) {
                return enumConstant.getName();
            }
        }
        if (value instanceof PsiField field && field instanceof PsiEnumConstant) {
            return field.getName();
        }
        String text = value.getText();
        int index = text.lastIndexOf('.');
        return index >= 0 ? text.substring(index + 1) : text;
    }

    private static String trimQuotes(String text) {
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}

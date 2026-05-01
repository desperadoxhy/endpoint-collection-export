package com.personal.brunohelper.openapi;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.personal.brunohelper.parser.AnnotationUtils;
import com.personal.brunohelper.parser.DocCommentUtil;
import com.personal.brunohelper.parser.PsiTypeSupport;
import com.personal.brunohelper.settings.BrunoHelperSettingsState;
import com.personal.brunohelper.util.FieldBlacklistUtil;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PsiTypeSchemaResolver {

    private static final Set<String> TRANSPARENT_WRAPPERS = Set.of(
            "org.springframework.http.ResponseEntity",
            "java.util.Optional"
    );

    private static final Set<String> TRANSPARENT_WRAPPER_SIMPLE_NAMES = Set.of(
            "Response",
            "Result",
            "AjaxResult",
            "ApiResult",
            "CommonResult",
            "FieldPermissionResult",
            "TableDataInfo"
    );

    private final Map<String, Schema<?>> componentSchemas = new LinkedHashMap<>();
    private final Map<String, String> componentNames = new LinkedHashMap<>();
    private final Set<String> inProgress = new LinkedHashSet<>();

    public Schema<?> resolveSchema(PsiType type) {
        if (type instanceof PsiArrayType arrayType) {
            if (PsiTypeSupport.isBinaryResponseType(type)) {
                return new BinarySchema();
            }
            return new ArraySchema().items(resolveSchema(arrayType.getComponentType()));
        }

        if (PsiTypeSupport.isBinaryResponseType(type) || PsiTypeSupport.isMultipartType(type)) {
            return new BinarySchema();
        }
        if (type.equals(PsiTypes.voidType()) || "void".equals(type.getCanonicalText()) || CommonClassNames.JAVA_LANG_VOID.equals(type.getCanonicalText())) {
            return null;
        }
        if (type.equals(PsiTypes.booleanType()) || CommonClassNames.JAVA_LANG_BOOLEAN.equals(type.getCanonicalText())) {
            return new BooleanSchema();
        }
        if (type.equals(PsiTypes.byteType()) || type.equals(PsiTypes.shortType()) || type.equals(PsiTypes.intType()) || type.equals(PsiTypes.longType())
                || CommonClassNames.JAVA_LANG_BYTE.equals(type.getCanonicalText())
                || CommonClassNames.JAVA_LANG_SHORT.equals(type.getCanonicalText())
                || CommonClassNames.JAVA_LANG_INTEGER.equals(type.getCanonicalText())
                || CommonClassNames.JAVA_LANG_LONG.equals(type.getCanonicalText())) {
            return new IntegerSchema();
        }
        if (type.equals(PsiTypes.floatType()) || type.equals(PsiTypes.doubleType())
                || CommonClassNames.JAVA_LANG_FLOAT.equals(type.getCanonicalText())
                || CommonClassNames.JAVA_LANG_DOUBLE.equals(type.getCanonicalText())
                || "java.math.BigDecimal".equals(type.getCanonicalText())
                || "java.math.BigInteger".equals(type.getCanonicalText())) {
            return new NumberSchema();
        }
        if (PsiTypeSupport.isSimpleType(type)) {
            return stringSchemaFor(type.getCanonicalText());
        }
        if (PsiTypeSupport.isCollectionType(type) && type instanceof PsiClassType classType) {
            if (classType.getParameters().length == 0) {
                return new ArraySchema().items(new ObjectSchema());
            }
            return new ArraySchema().items(resolveSchema(classType.getParameters()[0]));
        }
        if (PsiTypeSupport.isMapType(type) && type instanceof PsiClassType classType) {
            if (classType.getParameters().length <= 1) {
                return new MapSchema().additionalProperties(new ObjectSchema());
            }
            return new MapSchema().additionalProperties(resolveSchema(classType.getParameters()[1]));
        }
        if (!(type instanceof PsiClassType classType)) {
            return new ObjectSchema();
        }

        PsiClassType resolvedType = unwrapTransparentType(classType);
        PsiClass psiClass = resolvedType.resolve();
        if (psiClass == null) {
            return new ObjectSchema();
        }
        if (psiClass.isEnum()) {
            return enumSchema(psiClass);
        }
        return schemaReferenceForClassType(resolvedType, psiClass);
    }

    public PsiType unwrapResponseType(@Nullable PsiType type) {
        if (type == null) {
            return PsiTypes.voidType();
        }
        PsiType current = type;
        while (current instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            if (psiClass == null) {
                return current;
            }
            String qualifiedName = psiClass.getQualifiedName();
            String simpleName = psiClass.getName();
            PsiType[] parameters = classType.getParameters();
            boolean transparent = parameters.length == 1
                    && ((qualifiedName != null && TRANSPARENT_WRAPPERS.contains(qualifiedName))
                    || (simpleName != null && TRANSPARENT_WRAPPER_SIMPLE_NAMES.contains(simpleName)));
            if (!transparent) {
                return current;
            }
            current = parameters[0];
        }
        return current;
    }

    public List<PropertyDescriptor> expandObjectProperties(PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return List.of();
        }
        PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
        PsiClass psiClass = resolveResult.getElement();
        if (psiClass == null || psiClass.isEnum()) {
            return List.of();
        }
        PsiSubstitutor substitutor = resolveResult.getSubstitutor();
        Map<String, PropertyDescriptor> properties = new LinkedHashMap<>();
        List<String> blacklist = BrunoHelperSettingsState.getInstance().getFieldBlacklistPatterns();
        for (PsiField field : psiClass.getAllFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            String fieldName = field.getName();
            if (fieldName == null) {
                continue;
            }
            if (!blacklist.isEmpty() && FieldBlacklistUtil.isBlacklisted(fieldName, blacklist)) {
                continue;
            }
            PsiType fieldType = substitutor.substitute(field.getType());
            if (fieldType == null) {
                fieldType = field.getType();
            }
            properties.putIfAbsent(fieldName, new PropertyDescriptor(
                    field.getName(),
                    resolveSchema(fieldType),
                    AnnotationUtils.hasRequiredValidation(field),
                    fallbackDescription(field)
            ));
        }
        return new ArrayList<>(properties.values());
    }

    public Components buildComponents() {
        if (componentSchemas.isEmpty()) {
            return null;
        }
        Components components = new Components();
        for (Map.Entry<String, Schema<?>> entry : componentSchemas.entrySet()) {
            components.addSchemas(entry.getKey(), entry.getValue());
        }
        return components;
    }

    private Schema<?> schemaReferenceForClassType(PsiClassType classType, PsiClass psiClass) {
        String canonicalText = classType.getCanonicalText();
        String componentName = componentNames.computeIfAbsent(canonicalText, unused -> createComponentName(classType, psiClass));
        if (!componentSchemas.containsKey(componentName) && inProgress.add(canonicalText)) {
            componentSchemas.put(componentName, buildObjectSchema(classType, psiClass));
            inProgress.remove(canonicalText);
        }
        return new Schema<>().$ref("#/components/schemas/" + componentName);
    }

    private ObjectSchema buildObjectSchema(PsiClassType classType, PsiClass psiClass) {
        ObjectSchema schema = new ObjectSchema();
        String description = fallbackDescription(psiClass);
        if (!description.isBlank()) {
            schema.setDescription(description);
        }

        PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
        PsiSubstitutor substitutor = resolveResult.getSubstitutor();
        Set<String> requiredFields = new LinkedHashSet<>();
        List<String> blacklist = BrunoHelperSettingsState.getInstance().getFieldBlacklistPatterns();
        for (PsiField field : psiClass.getAllFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            String fieldName = field.getName();
            if (fieldName == null || schema.getProperties() != null && schema.getProperties().containsKey(fieldName)) {
                continue;
            }
            if (!blacklist.isEmpty() && FieldBlacklistUtil.isBlacklisted(fieldName, blacklist)) {
                continue;
            }
            PsiType fieldType = substitutor.substitute(field.getType());
            if (fieldType == null) {
                fieldType = field.getType();
            }
            Schema<?> propertySchema = resolveSchema(fieldType);
            if (propertySchema == null) {
                continue;
            }
            String fieldDescription = fallbackDescription(field);
            if (!fieldDescription.isBlank()) {
                propertySchema.setDescription(fieldDescription);
            }
            schema.addProperty(fieldName, propertySchema);
            if (AnnotationUtils.hasRequiredValidation(field)) {
                requiredFields.add(fieldName);
            }
        }
        if (!requiredFields.isEmpty()) {
            schema.setRequired(new ArrayList<>(requiredFields));
        }
        return schema;
    }

    private PsiClassType unwrapTransparentType(PsiClassType type) {
        PsiClassType current = type;
        while (true) {
            PsiClass psiClass = current.resolve();
            if (psiClass == null) {
                return current;
            }
            String qualifiedName = psiClass.getQualifiedName();
            String simpleName = psiClass.getName();
            PsiType[] parameters = current.getParameters();
            if (parameters.length == 1
                    && ((qualifiedName != null && TRANSPARENT_WRAPPERS.contains(qualifiedName))
                    || (simpleName != null && TRANSPARENT_WRAPPER_SIMPLE_NAMES.contains(simpleName)))
                    && parameters[0] instanceof PsiClassType parameterType) {
                current = parameterType;
                continue;
            }
            if (parameters.length == 1
                    && ((qualifiedName != null && TRANSPARENT_WRAPPERS.contains(qualifiedName))
                    || (simpleName != null && TRANSPARENT_WRAPPER_SIMPLE_NAMES.contains(simpleName)))) {
                return current;
            }
            return current;
        }
    }

    private String createComponentName(PsiClassType classType, PsiClass psiClass) {
        String baseName = classType.getParameters().length == 0
                ? psiClass.getName()
                : sanitizeComponentName(classType.getPresentableText());
        if (baseName == null || baseName.isBlank()) {
            baseName = "AnonymousSchema";
        }
        if (!componentSchemas.containsKey(baseName)) {
            return baseName;
        }
        return baseName + "_" + Integer.toHexString(classType.getCanonicalText().hashCode());
    }

    private String sanitizeComponentName(String text) {
        return text.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private Schema<?> enumSchema(PsiClass psiClass) {
        StringSchema schema = new StringSchema();
        for (PsiField field : psiClass.getFields()) {
            if (field instanceof com.intellij.psi.PsiEnumConstant) {
                schema.addEnumItemObject(field.getName());
            }
        }
        return schema;
    }

    private StringSchema stringSchemaFor(String canonicalText) {
        StringSchema schema = new StringSchema();
        if ("java.time.LocalDate".equals(canonicalText)) {
            schema.setFormat("date");
        } else if ("java.time.LocalDateTime".equals(canonicalText)
                || "java.time.OffsetDateTime".equals(canonicalText)
                || "java.time.ZonedDateTime".equals(canonicalText)
                || "java.util.Date".equals(canonicalText)) {
            schema.setFormat("date-time");
        } else if ("java.util.UUID".equals(canonicalText)) {
            schema.setFormat("uuid");
        } else if ("java.net.URI".equals(canonicalText) || "java.net.URL".equals(canonicalText)) {
            schema.setFormat("uri");
        }
        return schema;
    }

    private String fallbackDescription(PsiClass psiClass) {
        return DocCommentUtil.extractDescription(psiClass);
    }

    private String fallbackDescription(PsiField field) {
        String description = DocCommentUtil.extractDescription(field);
        if (!description.isBlank()) {
            return description;
        }
        String validationMessage = AnnotationUtils.getValidationMessage(field);
        return validationMessage == null ? "" : validationMessage;
    }

    public record PropertyDescriptor(String name, Schema<?> schema, boolean required, String description) {
    }
}

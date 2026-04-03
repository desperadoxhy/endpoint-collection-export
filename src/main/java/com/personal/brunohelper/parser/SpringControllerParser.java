package com.personal.brunohelper.parser;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.personal.brunohelper.model.ControllerExportModel;
import com.personal.brunohelper.model.EndpointExportModel;
import com.personal.brunohelper.model.EndpointParameterModel;
import com.personal.brunohelper.model.ParameterSource;
import com.personal.brunohelper.model.RequestBodyModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpringControllerParser {

    private static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
    private static final String POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";
    private static final String PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";
    private static final String DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping";
    private static final String PATCH_MAPPING = "org.springframework.web.bind.annotation.PatchMapping";
    private static final String REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam";
    private static final String PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable";
    private static final String REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader";
    private static final String REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";
    private static final String REQUEST_PART = "org.springframework.web.bind.annotation.RequestPart";
    private static final String MODEL_ATTRIBUTE = "org.springframework.web.bind.annotation.ModelAttribute";

    private static final String DEFAULT_NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";

    public ControllerExportModel parse(PsiClass controllerClass) {
        return parse(controllerClass, null);
    }

    public ControllerExportModel parse(PsiClass controllerClass, @Nullable PsiMethod targetMethod) {
        String description = resolveDescription(controllerClass, null);
        String summary = resolveSummary(controllerClass, controllerClass.getName());
        List<String> classPaths = resolveClassPaths(controllerClass);
        List<EndpointExportModel> endpoints = new ArrayList<>();

        for (PsiMethod method : controllerClass.getMethods()) {
            if (targetMethod != null && !method.equals(targetMethod)) {
                continue;
            }
            if (!method.hasModifierProperty(PsiModifier.PUBLIC) || method.isConstructor()) {
                continue;
            }
            EndpointExportModel endpoint = parseEndpoint(controllerClass, classPaths, method);
            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }

        return new ControllerExportModel(
                controllerClass.getName() == null ? "Controller" : controllerClass.getName(),
                summary,
                description,
                endpoints
        );
    }

    private @Nullable EndpointExportModel parseEndpoint(PsiClass controllerClass, List<String> classPaths, PsiMethod method) {
        PsiAnnotation mappingAnnotation = AnnotationUtils.findMethodAnnotationIncludingSupers(
                method,
                GET_MAPPING,
                POST_MAPPING,
                PUT_MAPPING,
                DELETE_MAPPING,
                PATCH_MAPPING,
                REQUEST_MAPPING
        );
        if (mappingAnnotation == null) {
            return null;
        }

        String description = resolveDescription(method, null);
        String summary = resolveSummary(method, method.getName());
        Map<String, String> paramDescriptions = resolveParameterDescriptions(method);

        List<EndpointParameterModel> parameters = new ArrayList<>();
        RequestBodyModel requestBody = null;

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (PsiTypeSupport.shouldIgnoreParameter(parameter.getType())) {
                continue;
            }

            ParsedParameter parsedParameter = parseParameter(method, parameter, paramDescriptions);
            if (parsedParameter.requestBody != null) {
                requestBody = parsedParameter.requestBody;
            }
            if (parsedParameter.parameter != null) {
                parameters.add(parsedParameter.parameter);
            }
        }

        Set<String> httpMethods = resolveHttpMethods(mappingAnnotation, parameters, requestBody);
        List<String> paths = combinePaths(classPaths, resolveMethodPaths(mappingAnnotation));

        return new EndpointExportModel(
                controllerClass.getName() + "." + method.getName(),
                summary,
                description,
                paths,
                httpMethods,
                parameters,
                requestBody,
                method.getReturnType()
        );
    }

    private ParsedParameter parseParameter(PsiMethod method, PsiParameter parameter, Map<String, String> paramDescriptions) {
        PsiType type = parameter.getType();
        String fallbackName = parameter.getName() == null ? "arg" : parameter.getName();
        String validationMessage = AnnotationUtils.getValidationMessage(parameter);
        String description = paramDescriptions.getOrDefault(fallbackName, validationMessage == null ? "" : validationMessage);

        PsiAnnotation requestBody = AnnotationUtils.findParameterAnnotationIncludingSupers(method, parameter, REQUEST_BODY);
        if (requestBody != null) {
            return new ParsedParameter(null, new RequestBodyModel(
                    type,
                    isRequired(requestBody, parameter, true),
                    description,
                    "application/json"
            ));
        }

        PsiAnnotation requestPart = AnnotationUtils.findParameterAnnotationIncludingSupers(method, parameter, REQUEST_PART);
        if (requestPart != null || PsiTypeSupport.isMultipartType(type)) {
            return new ParsedParameter(new EndpointParameterModel(
                    resolveParameterName(requestPart, fallbackName),
                    description,
                    isRequired(requestPart, parameter, true),
                    null,
                    ParameterSource.REQUEST_PART,
                    type
            ), null);
        }

        PsiAnnotation pathVariable = AnnotationUtils.findParameterAnnotationIncludingSupers(method, parameter, PATH_VARIABLE);
        if (pathVariable != null) {
            return new ParsedParameter(new EndpointParameterModel(
                    resolveParameterName(pathVariable, fallbackName),
                    description,
                    true,
                    null,
                    ParameterSource.PATH_VARIABLE,
                    type
            ), null);
        }

        PsiAnnotation requestHeader = AnnotationUtils.findParameterAnnotationIncludingSupers(method, parameter, REQUEST_HEADER);
        if (requestHeader != null) {
            return new ParsedParameter(new EndpointParameterModel(
                    resolveParameterName(requestHeader, fallbackName),
                    description,
                    isRequired(requestHeader, parameter, true),
                    AnnotationUtils.getStringAttribute(requestHeader, "defaultValue"),
                    ParameterSource.REQUEST_HEADER,
                    type
            ), null);
        }

        PsiAnnotation requestParam = AnnotationUtils.findParameterAnnotationIncludingSupers(method, parameter, REQUEST_PARAM);
        if (requestParam != null) {
            String defaultValue = AnnotationUtils.getStringAttribute(requestParam, "defaultValue");
            boolean required = isRequired(requestParam, parameter, true) && (defaultValue == null || DEFAULT_NONE.equals(defaultValue));
            return new ParsedParameter(new EndpointParameterModel(
                    resolveParameterName(requestParam, fallbackName),
                    description,
                    required,
                    DEFAULT_NONE.equals(defaultValue) ? null : defaultValue,
                    ParameterSource.REQUEST_PARAM,
                    type
            ), null);
        }

        PsiAnnotation modelAttribute = AnnotationUtils.findParameterAnnotationIncludingSupers(method, parameter, MODEL_ATTRIBUTE);
        if (modelAttribute != null) {
            return new ParsedParameter(new EndpointParameterModel(
                    resolveParameterName(modelAttribute, fallbackName),
                    description,
                    AnnotationUtils.hasRequiredValidation(parameter),
                    null,
                    ParameterSource.MODEL_ATTRIBUTE,
                    type
            ), null);
        }

        if (PsiTypeSupport.isSimpleType(type) || PsiTypeSupport.isMultipartType(type)) {
            ParameterSource source = PsiTypeSupport.isMultipartType(type) ? ParameterSource.REQUEST_PART : ParameterSource.IMPLICIT_SIMPLE;
            return new ParsedParameter(new EndpointParameterModel(
                    fallbackName,
                    description,
                    AnnotationUtils.hasRequiredValidation(parameter),
                    null,
                    source,
                    type
            ), null);
        }

        return new ParsedParameter(new EndpointParameterModel(
                fallbackName,
                description,
                AnnotationUtils.hasRequiredValidation(parameter),
                null,
                ParameterSource.IMPLICIT_MODEL,
                type
        ), null);
    }

    private boolean isRequired(@Nullable PsiAnnotation annotation, PsiParameter parameter, boolean defaultValue) {
        if (annotation == null) {
            return AnnotationUtils.hasRequiredValidation(parameter) || defaultValue;
        }
        return AnnotationUtils.hasRequiredValidation(parameter)
                || parameter.getType() instanceof PsiPrimitiveType
                || AnnotationUtils.getBooleanAttribute(annotation, "required", defaultValue);
    }

    private String resolveParameterName(@Nullable PsiAnnotation annotation, String fallbackName) {
        if (annotation == null) {
            return fallbackName;
        }
        String value = AnnotationUtils.getStringAttribute(annotation, "name", "value");
        return value == null || value.isBlank() ? fallbackName : value;
    }

    private List<String> resolveClassPaths(PsiClass controllerClass) {
        PsiAnnotation requestMapping = AnnotationUtils.findAnnotation(controllerClass, REQUEST_MAPPING);
        return requestMapping == null ? List.of("") : defaultIfEmpty(AnnotationUtils.getStringArrayAttribute(requestMapping, "path", "value"), "");
    }

    private List<String> resolveMethodPaths(PsiAnnotation mappingAnnotation) {
        return defaultIfEmpty(AnnotationUtils.getStringArrayAttribute(mappingAnnotation, "path", "value"), "");
    }

    private Set<String> resolveHttpMethods(
            PsiAnnotation mappingAnnotation,
            List<EndpointParameterModel> parameters,
            @Nullable RequestBodyModel requestBody
    ) {
        String qualifiedName = mappingAnnotation.getQualifiedName();
        if (GET_MAPPING.equals(qualifiedName)) {
            return Set.of("GET");
        }
        if (POST_MAPPING.equals(qualifiedName)) {
            return Set.of("POST");
        }
        if (PUT_MAPPING.equals(qualifiedName)) {
            return Set.of("PUT");
        }
        if (DELETE_MAPPING.equals(qualifiedName)) {
            return Set.of("DELETE");
        }
        if (PATCH_MAPPING.equals(qualifiedName)) {
            return Set.of("PATCH");
        }
        Set<String> methods = new LinkedHashSet<>(AnnotationUtils.getEnumArrayAttribute(mappingAnnotation, "method"));
        if (!methods.isEmpty()) {
            return methods;
        }
        if (requestBody != null || parameters.stream().anyMatch(this::requiresBodyHeuristic)) {
            return Set.of("POST");
        }
        return Set.of("GET");
    }

    private boolean requiresBodyHeuristic(EndpointParameterModel parameter) {
        return parameter.getSource() == ParameterSource.REQUEST_PART
                || parameter.getSource() == ParameterSource.MODEL_ATTRIBUTE
                || parameter.getSource() == ParameterSource.IMPLICIT_MODEL;
    }

    private String resolveDescription(com.intellij.psi.PsiDocCommentOwner owner, @Nullable String fallback) {
        String description = DocCommentUtil.extractDescription(owner);
        if (!description.isBlank()) {
            return description;
        }
        if (owner instanceof PsiMethod method) {
            for (PsiMethod superMethod : method.findSuperMethods()) {
                description = DocCommentUtil.extractDescription(superMethod);
                if (!description.isBlank()) {
                    return description;
                }
            }
        }
        return fallback == null ? "" : fallback;
    }

    private String resolveSummary(com.intellij.psi.PsiDocCommentOwner owner, @Nullable String fallback) {
        String summary = DocCommentUtil.extractSummary(owner);
        if (!summary.isBlank()) {
            return summary;
        }
        if (owner instanceof PsiMethod method) {
            for (PsiMethod superMethod : method.findSuperMethods()) {
                summary = DocCommentUtil.extractSummary(superMethod);
                if (!summary.isBlank()) {
                    return summary;
                }
            }
        }
        return fallback == null ? "" : fallback;
    }

    private Map<String, String> resolveParameterDescriptions(PsiMethod method) {
        Map<String, String> descriptions = DocCommentUtil.extractParameterDescriptions(method);
        if (!descriptions.isEmpty()) {
            return descriptions;
        }
        for (PsiMethod superMethod : method.findSuperMethods()) {
            descriptions = DocCommentUtil.extractParameterDescriptions(superMethod);
            if (!descriptions.isEmpty()) {
                return descriptions;
            }
        }
        return Map.of();
    }

    private List<String> combinePaths(List<String> classPaths, List<String> methodPaths) {
        return SpringRequestPathUtil.combinePaths(classPaths, methodPaths);
    }

    private List<String> defaultIfEmpty(List<String> values, String fallback) {
        return values.isEmpty() ? List.of(fallback) : values;
    }

    private record ParsedParameter(EndpointParameterModel parameter, RequestBodyModel requestBody) {
    }
}

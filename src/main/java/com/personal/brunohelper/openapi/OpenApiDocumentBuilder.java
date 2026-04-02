package com.personal.brunohelper.openapi;

import com.intellij.psi.PsiType;
import com.personal.brunohelper.model.ControllerExportModel;
import com.personal.brunohelper.model.EndpointExportModel;
import com.personal.brunohelper.model.EndpointParameterModel;
import com.personal.brunohelper.model.GeneratedOpenApiDocument;
import com.personal.brunohelper.model.ParameterSource;
import com.personal.brunohelper.model.RequestBodyModel;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OpenApiDocumentBuilder {

    public GeneratedOpenApiDocument build(ControllerExportModel model) {
        OpenAPI openApi = new OpenAPI();
        openApi.setInfo(new Info()
                .title(model.getControllerName() + " Export")
                .version("1.0.0")
                .description(model.getDescription().isBlank() ? model.getSummary() : model.getDescription()));
        openApi.addTagsItem(new Tag()
                .name(model.getControllerName())
                .description(model.getDescription().isBlank() ? model.getSummary() : model.getDescription()));

        PsiTypeSchemaResolver resolver = new PsiTypeSchemaResolver();
        Paths paths = new Paths();

        for (EndpointExportModel endpoint : model.getEndpoints()) {
            for (String path : endpoint.getPaths()) {
                PathItem pathItem = paths.get(path);
                if (pathItem == null) {
                    pathItem = new PathItem();
                    paths.addPathItem(path, pathItem);
                }
                for (String httpMethod : endpoint.getHttpMethods()) {
                    attachOperation(pathItem, httpMethod, buildOperation(endpoint, model.getControllerName(), resolver));
                }
            }
        }

        openApi.setPaths(paths);
        openApi.setComponents(resolver.buildComponents());
        return new GeneratedOpenApiDocument(model.getControllerName(), Json.pretty(openApi), model.getEndpoints().size());
    }

    private Operation buildOperation(
            EndpointExportModel endpoint,
            String controllerName,
            PsiTypeSchemaResolver resolver
    ) {
        Operation operation = new Operation();
        operation.setOperationId(endpoint.getOperationId());
        operation.setSummary(endpoint.getSummary());
        if (!endpoint.getDescription().isBlank()) {
            operation.setDescription(endpoint.getDescription());
        }
        operation.setTags(List.of(controllerName));

        boolean multipart = endpoint.getParameters().stream()
                .anyMatch(parameter -> parameter.getSource() == ParameterSource.REQUEST_PART);
        boolean formEncoded = !multipart
                && endpoint.getRequestBody() == null
                && endpoint.getParameters().stream().anyMatch(this::isFormObjectParameter);

        List<Parameter> parameters = new ArrayList<>();
        RequestBody requestBody = buildRequestBody(endpoint, resolver, multipart, formEncoded);

        for (EndpointParameterModel parameter : endpoint.getParameters()) {
            if (parameter.getSource() == ParameterSource.PATH_VARIABLE) {
                parameters.add(openApiParameter("path", parameter, resolver, true));
                continue;
            }
            if (parameter.getSource() == ParameterSource.REQUEST_HEADER) {
                parameters.add(openApiParameter("header", parameter, resolver, parameter.isRequired()));
                continue;
            }
            if (multipart && isMultipartField(parameter)) {
                continue;
            }
            if (formEncoded && isFormObjectParameter(parameter)) {
                continue;
            }
            if (parameter.getSource() == ParameterSource.MODEL_ATTRIBUTE || parameter.getSource() == ParameterSource.IMPLICIT_MODEL) {
                for (PsiTypeSchemaResolver.PropertyDescriptor property : resolver.expandObjectProperties(parameter.getType())) {
                    parameters.add(openApiParameter("query", property, parameter.getDescription()));
                }
                continue;
            }
            if (parameter.getSource() == ParameterSource.REQUEST_BODY) {
                continue;
            }
            parameters.add(openApiParameter("query", parameter, resolver, parameter.isRequired()));
        }

        if (!parameters.isEmpty()) {
            operation.setParameters(deduplicate(parameters));
        }
        if (requestBody != null) {
            operation.setRequestBody(requestBody);
        }
        operation.setResponses(buildResponses(endpoint, resolver));
        return operation;
    }

    private RequestBody buildRequestBody(
            EndpointExportModel endpoint,
            PsiTypeSchemaResolver resolver,
            boolean multipart,
            boolean formEncoded
    ) {
        RequestBodyModel explicitBody = endpoint.getRequestBody();
        if (explicitBody != null) {
            Schema<?> schema = resolver.resolveSchema(explicitBody.getType());
            if (schema == null) {
                return null;
            }
            return new RequestBody()
                    .required(explicitBody.isRequired())
                    .description(explicitBody.getDescription())
                    .content(new Content().addMediaType(explicitBody.getContentType(), new MediaType().schema(schema)));
        }

        if (multipart) {
            ObjectSchema schema = new ObjectSchema();
            Set<String> requiredFields = new LinkedHashSet<>();
            for (EndpointParameterModel parameter : endpoint.getParameters()) {
                if (!isMultipartField(parameter)) {
                    continue;
                }
                addSchemaProperty(schema, parameter.getName(), resolver.resolveSchema(parameter.getType()), parameter.getDescription(), parameter.isRequired(), requiredFields);
            }
            if (!requiredFields.isEmpty()) {
                schema.setRequired(new ArrayList<>(requiredFields));
            }
            return new RequestBody()
                    .required(!requiredFields.isEmpty())
                    .content(new Content().addMediaType("multipart/form-data", new MediaType().schema(schema)));
        }

        if (formEncoded) {
            ObjectSchema schema = new ObjectSchema();
            Set<String> requiredFields = new LinkedHashSet<>();
            for (EndpointParameterModel parameter : endpoint.getParameters()) {
                if (!isFormObjectParameter(parameter)) {
                    continue;
                }
                for (PsiTypeSchemaResolver.PropertyDescriptor property : resolver.expandObjectProperties(parameter.getType())) {
                    addSchemaProperty(schema, property.name(), property.schema(), property.description(), property.required(), requiredFields);
                }
            }
            if (schema.getProperties() == null || schema.getProperties().isEmpty()) {
                return null;
            }
            if (!requiredFields.isEmpty()) {
                schema.setRequired(new ArrayList<>(requiredFields));
            }
            return new RequestBody()
                    .required(!requiredFields.isEmpty())
                    .content(new Content().addMediaType("application/x-www-form-urlencoded", new MediaType().schema(schema)));
        }

        return null;
    }

    private ApiResponses buildResponses(EndpointExportModel endpoint, PsiTypeSchemaResolver resolver) {
        ApiResponses responses = new ApiResponses();
        PsiType responseType = resolver.unwrapResponseType(endpoint.getResponseType());
        Schema<?> responseSchema = resolver.resolveSchema(responseType);
        ApiResponse response = new ApiResponse().description("Success");
        if (responseSchema != null) {
            String contentType = responseSchema instanceof io.swagger.v3.oas.models.media.BinarySchema
                    ? "application/octet-stream"
                    : "application/json";
            response.setContent(new Content().addMediaType(contentType, new MediaType().schema(responseSchema)));
        }
        responses.addApiResponse("200", response);
        return responses;
    }

    private Parameter openApiParameter(String in, EndpointParameterModel parameter, PsiTypeSchemaResolver resolver, boolean required) {
        Schema<?> schema = resolver.resolveSchema(parameter.getType());
        Parameter openApiParameter = new Parameter()
                .name(parameter.getName())
                .in(in)
                .required(required)
                .schema(schema);
        if (!parameter.getDescription().isBlank()) {
            openApiParameter.setDescription(parameter.getDescription());
        }
        if (parameter.getDefaultValue() != null && schema != null) {
            schema.setDefault(parameter.getDefaultValue());
        }
        return openApiParameter;
    }

    private Parameter openApiParameter(String in, PsiTypeSchemaResolver.PropertyDescriptor property, String fallbackDescription) {
        Parameter parameter = new Parameter()
                .name(property.name())
                .in(in)
                .required(property.required())
                .schema(property.schema());
        String description = property.description().isBlank() ? fallbackDescription : property.description();
        if (description != null && !description.isBlank()) {
            parameter.setDescription(description);
        }
        return parameter;
    }

    private void attachOperation(PathItem pathItem, String httpMethod, Operation operation) {
        switch (httpMethod) {
            case "GET" -> pathItem.setGet(operation);
            case "POST" -> pathItem.setPost(operation);
            case "PUT" -> pathItem.setPut(operation);
            case "DELETE" -> pathItem.setDelete(operation);
            case "PATCH" -> pathItem.setPatch(operation);
            default -> pathItem.setPost(operation);
        }
    }

    private boolean isMultipartField(EndpointParameterModel parameter) {
        return parameter.getSource() == ParameterSource.REQUEST_PART
                || parameter.getSource() == ParameterSource.REQUEST_PARAM
                || parameter.getSource() == ParameterSource.IMPLICIT_SIMPLE
                || parameter.getSource() == ParameterSource.MODEL_ATTRIBUTE
                || parameter.getSource() == ParameterSource.IMPLICIT_MODEL;
    }

    private boolean isFormObjectParameter(EndpointParameterModel parameter) {
        return parameter.getSource() == ParameterSource.MODEL_ATTRIBUTE
                || parameter.getSource() == ParameterSource.IMPLICIT_MODEL;
    }

    private void addSchemaProperty(
            ObjectSchema schema,
            String name,
            Schema<?> propertySchema,
            String description,
            boolean required,
            Set<String> requiredFields
    ) {
        if (propertySchema == null) {
            return;
        }
        if (description != null && !description.isBlank()) {
            propertySchema.setDescription(description);
        }
        schema.addProperties(name, propertySchema);
        if (required) {
            requiredFields.add(name);
        }
    }

    private List<Parameter> deduplicate(List<Parameter> parameters) {
        List<Parameter> result = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (Parameter parameter : parameters) {
            String key = parameter.getIn() + ":" + parameter.getName();
            if (keys.add(key)) {
                result.add(parameter);
            }
        }
        return result;
    }
}

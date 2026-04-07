package com.personal.brunohelper.service;

import com.intellij.psi.PsiType;
import com.personal.brunohelper.model.ControllerExportModel;
import com.personal.brunohelper.model.EndpointExportModel;
import com.personal.brunohelper.model.EndpointParameterModel;
import com.personal.brunohelper.model.ExportEndpointResult;
import com.personal.brunohelper.model.ExportEndpointStatus;
import com.personal.brunohelper.model.ParameterSource;
import com.personal.brunohelper.model.RequestBodyModel;
import com.personal.brunohelper.openapi.PsiTypeSchemaResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BrunoCollectionWriter {

    private static final String COLLECTION_FILE = "opencollection.yml";
    private static final String FOLDER_FILE = "folder.yml";
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^}/]+)}");
    private static final Map<String, Integer> HTTP_METHOD_ORDER = Map.of(
            "GET", 1,
            "POST", 2,
            "PUT", 3,
            "PATCH", 4,
            "DELETE", 5,
            "OPTIONS", 6,
            "HEAD", 7
    );

    public GenerationResult writeCollection(
            ControllerExportModel model,
            String projectName,
            Path projectDirectory,
            Path controllerDirectory
    ) throws IOException {
        Path baseOutputDirectory = projectDirectory.getParent() == null ? projectDirectory : projectDirectory.getParent();
        return writePreparedCollection(
                prepareCollection(
                        model,
                        projectName,
                        projectDirectory,
                        controllerDirectory,
                        BrunoExportOptions.resolveWorkspaceFile(baseOutputDirectory)
                )
        );
    }

    PreparedCollection prepareCollection(
            ControllerExportModel model,
            String projectName,
            Path projectDirectory,
            Path controllerDirectory,
            Path workspaceFile
    ) {
        String collectionName = projectName == null || projectName.isBlank() ? "project" : projectName;
        String controllerDisplayName = model.getSummary() == null || model.getSummary().isBlank()
                ? model.getControllerName()
                : model.getSummary();
        return new PreparedCollection(
                collectionName,
                controllerDisplayName,
                projectDirectory,
                controllerDirectory,
                workspaceFile,
                buildRequestFiles(model)
        );
    }

    GenerationResult writePreparedCollection(PreparedCollection preparedCollection) throws IOException {
        Files.createDirectories(preparedCollection.projectDirectory());
        Files.createDirectories(preparedCollection.controllerDirectory());

        writeWorkspaceFile(
                preparedCollection.workspaceFile(),
                preparedCollection.collectionName(),
                preparedCollection.projectDirectory()
        );
        writeProjectCollectionFileIfMissing(preparedCollection.projectDirectory(), preparedCollection.collectionName());
        writeFile(
                preparedCollection.controllerDirectory().resolve(FOLDER_FILE),
                renderFolderFile(preparedCollection.controllerDisplayName())
        );

        int sequence = 1;
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<ExportEndpointResult> endpointResults = new ArrayList<>();
        for (RequestFile requestFile : preparedCollection.requestFiles()) {
            String fileName = requestFile.fileName();
            Path requestFilePath = preparedCollection.controllerDirectory().resolve(fileName);
            if (Files.exists(requestFilePath)) {
                skippedCount++;
                endpointResults.add(new ExportEndpointResult(
                        requestFile.relativeUrl(),
                        requestFile.methodName(),
                        requestFile.endpointName(),
                        ExportEndpointStatus.SKIPPED,
                        fileName,
                        "接口文件已存在，已跳过。"
                ));
                sequence++;
                continue;
            }
            try {
                writeFile(requestFilePath, renderRequestFile(requestFile, sequence));
                createdCount++;
                endpointResults.add(new ExportEndpointResult(
                        requestFile.relativeUrl(),
                        requestFile.methodName(),
                        requestFile.endpointName(),
                        ExportEndpointStatus.SUCCESS,
                        fileName,
                        null
                ));
            } catch (IOException exception) {
                failedCount++;
                endpointResults.add(new ExportEndpointResult(
                        requestFile.relativeUrl(),
                        requestFile.methodName(),
                        requestFile.endpointName(),
                        ExportEndpointStatus.FAILED,
                        fileName,
                        exception.getMessage()
                ));
            }
            sequence++;
        }

        return new GenerationResult(
                preparedCollection.collectionName(),
                preparedCollection.projectDirectory(),
                preparedCollection.controllerDirectory(),
                createdCount,
                skippedCount,
                failedCount,
                List.copyOf(endpointResults)
        );
    }

    private List<RequestFile> buildRequestFiles(ControllerExportModel model) {
        List<RequestFile> requestFiles = new ArrayList<>();
        for (EndpointExportModel endpoint : model.getEndpoints()) {
            List<String> methods = endpoint.getHttpMethods().stream()
                    .sorted(Comparator.comparingInt(method -> HTTP_METHOD_ORDER.getOrDefault(method, Integer.MAX_VALUE)))
                    .toList();
            for (String path : endpoint.getPaths()) {
                for (String method : methods) {
                    requestFiles.add(buildRequestFile(endpoint, method, path));
                }
            }
        }
        return requestFiles;
    }

    private RequestFile buildRequestFile(EndpointExportModel endpoint, String method, String rawPath) {
        PsiTypeSchemaResolver resolver = new PsiTypeSchemaResolver();
        String path = normalizeRequestPath(rawPath);
        String requestName = buildRequestName(endpoint, method, path);

        boolean multipart = endpoint.getParameters().stream().anyMatch(this::isMultipartField);
        boolean formEncoded = !multipart
                && endpoint.getRequestBody() == null
                && endpoint.getParameters().stream().anyMatch(this::isFormObjectParameter);

        List<ParamEntry> params = new ArrayList<>();
        List<HeaderEntry> headers = new ArrayList<>();

        for (EndpointParameterModel parameter : endpoint.getParameters()) {
            if (parameter.getSource() == ParameterSource.PATH_VARIABLE) {
                params.add(new ParamEntry(parameter.getName(), parameterValue(parameter, resolver), "path"));
                continue;
            }
            if (parameter.getSource() == ParameterSource.REQUEST_HEADER) {
                headers.add(new HeaderEntry(parameter.getName(), parameterValue(parameter, resolver)));
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
                    params.add(new ParamEntry(property.name(), scalarValue(property.schema(), resolver.buildComponents()), "query"));
                }
                continue;
            }
            if (parameter.getSource() == ParameterSource.REQUEST_BODY) {
                continue;
            }
            params.add(new ParamEntry(parameter.getName(), parameterValue(parameter, resolver), "query"));
        }

        BodyEntry body = buildBody(endpoint, resolver, multipart, formEncoded);
        if (body != null && body.type().equals("json")) {
            addHeaderIfAbsent(headers, "Content-Type", "application/json");
        }
        if (body != null && body.type().equals("form-urlencoded")) {
            addHeaderIfAbsent(headers, "Content-Type", "application/x-www-form-urlencoded");
        }
        if (body != null && body.type().equals("text")) {
            addHeaderIfAbsent(headers, "Content-Type", "text/plain");
        }
        if (body != null && body.type().equals("xml")) {
            addHeaderIfAbsent(headers, "Content-Type", "application/xml");
        }

        return new RequestFile(
                requestName,
                path,
                extractMethodName(endpoint.getOperationId()),
                buildFileSlug(method, path, endpoint.getOperationId()) + ".yml",
                method,
                "{{baseUrl}}" + path,
                params,
                headers,
                body,
                buildDocs(endpoint)
        );
    }

    private String buildRequestName(EndpointExportModel endpoint, String method, String path) {
        String summary = endpoint.getSummary();
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        return method + " " + path;
    }

    private String buildFileSlug(String method, String path, String operationId) {
        String basis = method + "-" + path;
        if (operationId != null && !operationId.isBlank()) {
            basis = basis + "-" + operationId;
        }
        return BrunoExportOptions.sanitizeFileSystemName(basis);
    }

    private String extractMethodName(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            return "";
        }
        int separatorIndex = operationId.lastIndexOf('.');
        return separatorIndex >= 0 && separatorIndex < operationId.length() - 1
                ? operationId.substring(separatorIndex + 1)
                : operationId;
    }

    private String buildDocs(EndpointExportModel endpoint) {
        if (!endpoint.getDescription().isBlank()) {
            return endpoint.getDescription();
        }
        return endpoint.getSummary();
    }

    private BodyEntry buildBody(
            EndpointExportModel endpoint,
            PsiTypeSchemaResolver resolver,
            boolean multipart,
            boolean formEncoded
    ) {
        RequestBodyModel explicitBody = endpoint.getRequestBody();
        if (explicitBody != null) {
            String bodyType = resolveBodyType(explicitBody.getContentType());
            if ("json".equals(bodyType)) {
                Object example = exampleValue(explicitBody.getType(), resolver);
                return new BodyEntry("json", Json.pretty(example == null ? Map.of() : example), List.of());
            }
            return new BodyEntry(bodyType, "", List.of());
        }

        if (multipart) {
            List<FormEntry> formEntries = new ArrayList<>();
            for (EndpointParameterModel parameter : endpoint.getParameters()) {
                if (!isMultipartField(parameter)) {
                    continue;
                }
                if (parameter.getSource() == ParameterSource.MODEL_ATTRIBUTE || parameter.getSource() == ParameterSource.IMPLICIT_MODEL) {
                    for (PsiTypeSchemaResolver.PropertyDescriptor property : resolver.expandObjectProperties(parameter.getType())) {
                        formEntries.add(new FormEntry(property.name(), scalarValue(property.schema(), resolver.buildComponents())));
                    }
                } else {
                    formEntries.add(new FormEntry(parameter.getName(), parameterValue(parameter, resolver)));
                }
            }
            return formEntries.isEmpty() ? null : new BodyEntry("multipart-form", "", formEntries);
        }

        if (formEncoded) {
            List<FormEntry> formEntries = new ArrayList<>();
            for (EndpointParameterModel parameter : endpoint.getParameters()) {
                if (!isFormObjectParameter(parameter)) {
                    continue;
                }
                for (PsiTypeSchemaResolver.PropertyDescriptor property : resolver.expandObjectProperties(parameter.getType())) {
                    formEntries.add(new FormEntry(property.name(), scalarValue(property.schema(), resolver.buildComponents())));
                }
            }
            return formEntries.isEmpty() ? null : new BodyEntry("form-urlencoded", "", formEntries);
        }

        return null;
    }

    private Object exampleValue(PsiType type, PsiTypeSchemaResolver resolver) {
        Schema<?> schema = resolver.resolveSchema(type);
        Components components = resolver.buildComponents();
        Map<String, Schema> schemas = components == null || components.getSchemas() == null
                ? Map.of()
                : components.getSchemas();
        return exampleValue(schema, schemas, Set.of());
    }

    private Object exampleValue(Schema<?> schema, Map<String, Schema> components, Set<String> visitingRefs) {
        if (schema == null) {
            return null;
        }
        if (schema.get$ref() != null) {
            String reference = schema.get$ref();
            String componentName = reference.substring(reference.lastIndexOf('/') + 1);
            if (visitingRefs.contains(componentName)) {
                return Map.of();
            }
            Schema<?> componentSchema = components.get(componentName);
            if (componentSchema == null) {
                return Map.of();
            }
            java.util.LinkedHashSet<String> nextRefs = new java.util.LinkedHashSet<>(visitingRefs);
            nextRefs.add(componentName);
            return exampleValue(componentSchema, components, nextRefs);
        }
        if (schema instanceof ArraySchema arraySchema) {
            Object itemValue = exampleValue(arraySchema.getItems(), components, visitingRefs);
            return itemValue == null ? List.of() : List.of(itemValue);
        }
        if (schema instanceof MapSchema mapSchema) {
            Object value = exampleValue((Schema<?>) mapSchema.getAdditionalProperties(), components, visitingRefs);
            return Map.of("key", value == null ? "" : value);
        }
        if (schema instanceof BinarySchema) {
            return "<binary>";
        }
        if (schema instanceof BooleanSchema) {
            return Boolean.TRUE;
        }
        if (schema instanceof IntegerSchema) {
            return 0;
        }
        if (schema instanceof NumberSchema) {
            return 0.0;
        }
        if (schema instanceof ObjectSchema || schema.getProperties() != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                    Object value = exampleValue(entry.getValue(), components, visitingRefs);
                    result.put(entry.getKey(), value == null ? "" : value);
                }
            }
            return result;
        }
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return String.valueOf(schema.getEnum().get(0));
        }
        return "";
    }

    private String scalarValue(Schema<?> schema, Components components) {
        Map<String, Schema> componentMap = components == null || components.getSchemas() == null
                ? Map.of()
                : components.getSchemas();
        Object value = exampleValue(schema, componentMap, Set.of());
        return value == null ? "" : String.valueOf(value);
    }

    private String parameterValue(EndpointParameterModel parameter, PsiTypeSchemaResolver resolver) {
        if (parameter.getDefaultValue() != null && !parameter.getDefaultValue().isBlank()) {
            return parameter.getDefaultValue();
        }
        return scalarValue(resolver.resolveSchema(parameter.getType()), resolver.buildComponents());
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

    private void addHeaderIfAbsent(List<HeaderEntry> headers, String name, String value) {
        boolean exists = headers.stream().anyMatch(header -> header.name().equalsIgnoreCase(name));
        if (!exists) {
            headers.add(new HeaderEntry(name, value));
        }
    }

    private String resolveBodyType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "json";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("json")) {
            return "json";
        }
        if (normalized.contains("xml")) {
            return "xml";
        }
        if (normalized.contains("x-www-form-urlencoded")) {
            return "form-urlencoded";
        }
        if (normalized.contains("multipart/form-data")) {
            return "multipart-form";
        }
        return "text";
    }

    private String normalizeRequestPath(String rawPath) {
        String normalized = rawPath == null || rawPath.isBlank() ? "/" : rawPath;
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        Matcher matcher = PATH_VARIABLE_PATTERN.matcher(normalized);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ":" + matcher.group(1));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void writeProjectCollectionFileIfMissing(Path projectDirectory, String collectionName) throws IOException {
        Path collectionFile = projectDirectory.resolve(COLLECTION_FILE);
        if (!Files.exists(collectionFile)) {
            writeFile(collectionFile, renderCollectionFile(collectionName));
        }
    }

    private void writeWorkspaceFile(Path workspaceFile, String collectionName, Path projectDirectory) throws IOException {
        Path workspaceDirectory = workspaceFile.getParent();
        if (workspaceDirectory != null) {
            Files.createDirectories(workspaceDirectory);
        }

        String workspaceName = resolveWorkspaceName(workspaceDirectory);
        String collectionPath = toWorkspaceRelativePath(workspaceDirectory, projectDirectory);
        WorkspaceDocument workspace = Files.exists(workspaceFile)
                ? parseWorkspaceFile(Files.readString(workspaceFile, StandardCharsets.UTF_8), workspaceName)
                : new WorkspaceDocument(workspaceName, new ArrayList<>(), null);

        Map<String, WorkspaceCollectionEntry> collectionsByPath = new LinkedHashMap<>();
        for (WorkspaceCollectionEntry entry : workspace.collections()) {
            collectionsByPath.putIfAbsent(entry.path(), entry);
        }
        collectionsByPath.put(collectionPath, new WorkspaceCollectionEntry(collectionName, collectionPath));
        List<WorkspaceCollectionEntry> updatedCollections = new ArrayList<>(collectionsByPath.values());

        writeFile(
                workspaceFile,
                renderWorkspaceFile(workspace.workspaceName(), updatedCollections, workspace.activeEnvironmentUid())
        );
    }

    private String renderCollectionFile(String collectionName) {
        StringBuilder builder = new StringBuilder();
        builder.append("opencollection: 1.0.0\n\n");
        builder.append("info:\n");
        builder.append("  name: ").append(yamlString(collectionName)).append('\n');
        builder.append("bundled: false\n");
        builder.append("extensions:\n");
        builder.append("  bruno:\n");
        builder.append("    ignore:\n");
        builder.append("      - node_modules\n");
        builder.append("      - .git\n");
        return builder.toString();
    }

    private WorkspaceDocument parseWorkspaceFile(String content, String defaultWorkspaceName) {
        String workspaceName = defaultWorkspaceName;
        String activeEnvironmentUid = null;
        List<WorkspaceCollectionEntry> collections = new ArrayList<>();
        String pendingCollectionName = null;
        boolean inInfo = false;
        boolean inCollections = false;

        String normalized = content == null ? "" : content.replace("\r\n", "\n");
        for (String line : normalized.split("\n", -1)) {
            if ("info:".equals(line)) {
                inInfo = true;
                inCollections = false;
                continue;
            }
            if ("collections:".equals(line)) {
                inCollections = true;
                inInfo = false;
                pendingCollectionName = null;
                continue;
            }
            if (line.startsWith("activeEnvironmentUid:")) {
                activeEnvironmentUid = line.substring("activeEnvironmentUid:".length()).trim();
            }
            if (inInfo) {
                if (line.startsWith("  name:")) {
                    workspaceName = parseYamlScalar(line.substring("  name:".length()).trim(), defaultWorkspaceName);
                    continue;
                }
                if (!line.startsWith("  ") && !line.isBlank()) {
                    inInfo = false;
                }
            }
            if (inCollections) {
                if (line.startsWith("  - name:")) {
                    pendingCollectionName = parseYamlScalar(line.substring("  - name:".length()).trim(), "");
                    continue;
                }
                if (line.startsWith("    path:") && pendingCollectionName != null) {
                    String path = parseYamlScalar(line.substring("    path:".length()).trim(), "");
                    collections.add(new WorkspaceCollectionEntry(pendingCollectionName, path));
                    pendingCollectionName = null;
                    continue;
                }
                if (!line.startsWith("  ") && !line.isBlank()) {
                    inCollections = false;
                }
            }
        }

        return new WorkspaceDocument(workspaceName, collections, activeEnvironmentUid);
    }

    private String renderFolderFile(String controllerDisplayName) {
        StringBuilder builder = new StringBuilder();
        builder.append("info:\n");
        builder.append("  name: ").append(yamlString(controllerDisplayName)).append('\n');
        return builder.toString();
    }

    private String renderWorkspaceFile(
            String workspaceName,
            List<WorkspaceCollectionEntry> collections,
            String activeEnvironmentUid
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("opencollection: 1.0.0\n");
        builder.append("info:\n");
        builder.append("  name: ").append(yamlString(workspaceName)).append('\n');
        builder.append("  type: workspace\n\n");
        builder.append("collections:\n");
        for (WorkspaceCollectionEntry entry : collections) {
            builder.append("  - name: ").append(yamlString(entry.name())).append('\n');
            builder.append("    path: ").append(yamlString(entry.path())).append('\n');
        }
        builder.append("\nspecs:\n\n");
        builder.append("docs: ''\n");
        if (activeEnvironmentUid != null && !activeEnvironmentUid.isBlank()) {
            builder.append("\nactiveEnvironmentUid: ").append(activeEnvironmentUid).append('\n');
        }
        return builder.toString();
    }

    private String renderRequestFile(RequestFile requestFile, int sequence) {
        StringBuilder builder = new StringBuilder();
        builder.append("info:\n");
        builder.append("  name: ").append(yamlString(requestFile.name())).append('\n');
        builder.append("  type: http\n");
        builder.append("  seq: ").append(sequence).append("\n\n");

        builder.append("http:\n");
        builder.append("  method: ").append(requestFile.method()).append('\n');
        builder.append("  url: ").append(yamlString(requestFile.url())).append('\n');

        if (!requestFile.params().isEmpty()) {
            builder.append("  params:\n");
            for (ParamEntry param : requestFile.params()) {
                builder.append("    - name: ").append(yamlString(param.name())).append('\n');
                builder.append("      value: ").append(yamlString(param.value())).append('\n');
                builder.append("      type: ").append(yamlString(param.type())).append('\n');
            }
        }

        if (!requestFile.headers().isEmpty()) {
            builder.append("  headers:\n");
            for (HeaderEntry header : requestFile.headers()) {
                builder.append("    - name: ").append(yamlString(header.name())).append('\n');
                builder.append("      value: ").append(yamlString(header.value())).append('\n');
            }
        }

        if (requestFile.body() != null) {
            builder.append("  body:\n");
            builder.append("    type: ").append(yamlString(requestFile.body().type())).append('\n');
            if ("json".equals(requestFile.body().type())) {
                builder.append("    data: |-\n");
                appendIndentedBlock(builder, requestFile.body().rawData(), 6);
            } else if (!requestFile.body().formData().isEmpty()) {
                builder.append("    data:\n");
                for (FormEntry formEntry : requestFile.body().formData()) {
                    builder.append("      - name: ").append(yamlString(formEntry.name())).append('\n');
                    builder.append("        value: ").append(yamlString(formEntry.value())).append('\n');
                }
            } else if (!requestFile.body().rawData().isBlank()) {
                builder.append("    data: |-\n");
                appendIndentedBlock(builder, requestFile.body().rawData(), 6);
            }
        }

        builder.append("\nsettings:\n");
        builder.append("  encodeUrl: true\n");
        builder.append("  timeout: 0\n");
        builder.append("  followRedirects: true\n");
        builder.append("  maxRedirects: 5\n");

        if (requestFile.docs() != null && !requestFile.docs().isBlank()) {
            builder.append("\ndocs: |-\n");
            appendIndentedBlock(builder, requestFile.docs(), 2);
        }

        return builder.toString();
    }

    private void appendIndentedBlock(StringBuilder builder, String text, int indent) {
        String prefix = " ".repeat(indent);
        String normalized = text == null ? "" : text.replace("\r\n", "\n");
        for (String line : normalized.split("\n", -1)) {
            builder.append(prefix).append(line).append('\n');
        }
    }

    private String resolveWorkspaceName(Path workspaceDirectory) {
        if (workspaceDirectory == null || workspaceDirectory.getFileName() == null) {
            return "workspace";
        }
        return workspaceDirectory.getFileName().toString();
    }

    private String toWorkspaceRelativePath(Path workspaceDirectory, Path projectDirectory) {
        Path relativePath;
        if (workspaceDirectory == null) {
            relativePath = projectDirectory;
        } else {
            try {
                relativePath = workspaceDirectory.relativize(projectDirectory);
            } catch (IllegalArgumentException exception) {
                relativePath = projectDirectory;
            }
        }
        return relativePath.toString().replace('\\', '/');
    }

    private String parseYamlScalar(String rawValue, String fallbackValue) {
        if (rawValue == null) {
            return fallbackValue;
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return fallbackValue;
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\\", "\\");
        }
        return trimmed;
    }

    private String yamlString(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    public record GenerationResult(
            String collectionName,
            Path projectDirectory,
            Path controllerDirectory,
            int createdRequestCount,
            int skippedRequestCount,
            int failedRequestCount,
            List<ExportEndpointResult> endpointResults
    ) {
    }

    record PreparedCollection(
            String collectionName,
            String controllerDisplayName,
            Path projectDirectory,
            Path controllerDirectory,
            Path workspaceFile,
            List<RequestFile> requestFiles
    ) {
    }

    private record WorkspaceDocument(
            String workspaceName,
            List<WorkspaceCollectionEntry> collections,
            String activeEnvironmentUid
    ) {
    }

    private record WorkspaceCollectionEntry(String name, String path) {
    }

    private record RequestFile(
            String endpointName,
            String relativeUrl,
            String methodName,
            String fileName,
            String method,
            String url,
            List<ParamEntry> params,
            List<HeaderEntry> headers,
            BodyEntry body,
            String docs
    ) {
        String name() {
            return endpointName;
        }
    }

    private record ParamEntry(String name, String value, String type) {
    }

    private record HeaderEntry(String name, String value) {
    }

    private record FormEntry(String name, String value) {
    }

    private record BodyEntry(String type, String rawData, List<FormEntry> formData) {
    }
}

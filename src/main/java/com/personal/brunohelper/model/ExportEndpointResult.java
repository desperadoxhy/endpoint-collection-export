package com.personal.brunohelper.model;

import org.jetbrains.annotations.Nullable;

public record ExportEndpointResult(
        String relativeUrl,
        String methodName,
        String endpointName,
        ExportEndpointStatus status,
        String yamlFileName,
        @Nullable String message
) {
}

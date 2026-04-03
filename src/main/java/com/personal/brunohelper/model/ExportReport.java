package com.personal.brunohelper.model;

import java.util.List;

public record ExportReport(
        String serviceName,
        String className,
        int controllerEndpointCount,
        int exportedEndpointCount,
        int skippedEndpointCount,
        int succeededEndpointCount,
        List<ExportEndpointResult> endpointResults
) {
}

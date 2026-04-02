package com.personal.brunohelper.model;

import com.intellij.psi.PsiType;

import java.util.List;
import java.util.Set;

public final class EndpointExportModel {

    private final String operationId;
    private final String summary;
    private final String description;
    private final List<String> paths;
    private final Set<String> httpMethods;
    private final List<EndpointParameterModel> parameters;
    private final RequestBodyModel requestBody;
    private final PsiType responseType;

    public EndpointExportModel(
            String operationId,
            String summary,
            String description,
            List<String> paths,
            Set<String> httpMethods,
            List<EndpointParameterModel> parameters,
            RequestBodyModel requestBody,
            PsiType responseType
    ) {
        this.operationId = operationId;
        this.summary = summary;
        this.description = description;
        this.paths = paths;
        this.httpMethods = httpMethods;
        this.parameters = parameters;
        this.requestBody = requestBody;
        this.responseType = responseType;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPaths() {
        return paths;
    }

    public Set<String> getHttpMethods() {
        return httpMethods;
    }

    public List<EndpointParameterModel> getParameters() {
        return parameters;
    }

    public RequestBodyModel getRequestBody() {
        return requestBody;
    }

    public PsiType getResponseType() {
        return responseType;
    }
}

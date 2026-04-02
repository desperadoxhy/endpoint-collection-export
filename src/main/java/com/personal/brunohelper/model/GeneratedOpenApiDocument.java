package com.personal.brunohelper.model;

public final class GeneratedOpenApiDocument {

    private final String controllerName;
    private final String json;
    private final int endpointCount;

    public GeneratedOpenApiDocument(String controllerName, String json, int endpointCount) {
        this.controllerName = controllerName;
        this.json = json;
        this.endpointCount = endpointCount;
    }

    public String getControllerName() {
        return controllerName;
    }

    public String getJson() {
        return json;
    }

    public int getEndpointCount() {
        return endpointCount;
    }
}

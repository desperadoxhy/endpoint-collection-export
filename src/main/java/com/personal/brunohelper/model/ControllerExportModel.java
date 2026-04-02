package com.personal.brunohelper.model;

import java.util.List;

public final class ControllerExportModel {

    private final String controllerName;
    private final String summary;
    private final String description;
    private final List<EndpointExportModel> endpoints;

    public ControllerExportModel(String controllerName, String summary, String description, List<EndpointExportModel> endpoints) {
        this.controllerName = controllerName;
        this.summary = summary;
        this.description = description;
        this.endpoints = endpoints;
    }

    public String getControllerName() {
        return controllerName;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public List<EndpointExportModel> getEndpoints() {
        return endpoints;
    }
}

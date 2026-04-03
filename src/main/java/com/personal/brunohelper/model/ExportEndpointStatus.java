package com.personal.brunohelper.model;

public enum ExportEndpointStatus {
    SUCCESS("成功"),
    FAILED("失败"),
    SKIPPED("跳过");

    private final String displayName;

    ExportEndpointStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package com.personal.brunohelper.model;

import org.jetbrains.annotations.Nullable;

public final class ExportOutcome {

    private final boolean success;
    private final String message;
    private final ExportReport report;

    private ExportOutcome(boolean success, String message, @Nullable ExportReport report) {
        this.success = success;
        this.message = message;
        this.report = report;
    }

    public static ExportOutcome success(String message) {
        return new ExportOutcome(true, message, null);
    }

    public static ExportOutcome success(String message, ExportReport report) {
        return new ExportOutcome(true, message, report);
    }

    public static ExportOutcome failure(String message) {
        return new ExportOutcome(false, message, null);
    }

    public static ExportOutcome failure(String message, ExportReport report) {
        return new ExportOutcome(false, message, report);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public @Nullable ExportReport getReport() {
        return report;
    }
}

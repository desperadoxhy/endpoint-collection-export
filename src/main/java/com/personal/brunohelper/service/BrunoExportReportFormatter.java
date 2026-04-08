package com.personal.brunohelper.service;

import com.personal.brunohelper.i18n.BrunoHelperBundle;
import com.personal.brunohelper.model.ExportEndpointResult;
import com.personal.brunohelper.model.ExportReport;

import java.util.ArrayList;
import java.util.List;

public final class BrunoExportReportFormatter {

    public String format(ExportReport report) {
        return formatSummary(report) + formatDirectorySummary(report) + '\n' + formatTable(report);
    }

    public String formatSummary(ExportReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append(BrunoHelperBundle.message("export.report.title")).append('\n').append('\n');
        builder.append(BrunoHelperBundle.message("export.report.summary.service")).append(' ').append(report.serviceName()).append('\n');
        builder.append(BrunoHelperBundle.message("export.report.summary.class")).append(' ').append(report.className()).append('\n');
        builder.append(BrunoHelperBundle.message("export.report.summary.total")).append(' ').append(report.controllerEndpointCount()).append('\n');
        builder.append(BrunoHelperBundle.message("export.report.summary.selected")).append(' ').append(report.exportedEndpointCount()).append('\n');
        builder.append(BrunoHelperBundle.message("export.report.summary.skipped")).append(' ').append(report.skippedEndpointCount()).append('\n');
        builder.append(BrunoHelperBundle.message("export.report.summary.success")).append(' ').append(report.succeededEndpointCount()).append('\n');
        builder.append(BrunoHelperBundle.message("export.report.summary.failed")).append(' ').append(report.failedEndpointCount()).append('\n');
        return builder.toString();
    }

    public String formatTable(ExportReport report) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(
                BrunoHelperBundle.message("export.report.table.relativeUrl"),
                BrunoHelperBundle.message("export.report.table.methodName"),
                BrunoHelperBundle.message("export.report.table.exportResult"),
                BrunoHelperBundle.message("export.report.table.endpointName")
        ));
        for (ExportEndpointResult endpointResult : report.endpointResults()) {
            rows.add(new Row(
                    endpointResult.relativeUrl(),
                    endpointResult.methodName(),
                    endpointResult.status().getDisplayName(),
                    endpointResult.endpointName()
            ));
        }

        int urlWidth = columnWidth(rows, Row::relativeUrl);
        int methodWidth = columnWidth(rows, Row::methodName);
        int statusWidth = columnWidth(rows, Row::status);
        int nameWidth = columnWidth(rows, Row::endpointName);

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            builder.append(padRight(row.relativeUrl(), urlWidth))
                    .append(" | ").append(padRight(row.methodName(), methodWidth))
                    .append(" | ").append(padRight(row.status(), statusWidth))
                    .append(" | ").append(padRight(row.endpointName(), nameWidth))
                    .append('\n');
            if (index == 0) {
                builder.append(repeat('-', urlWidth))
                        .append(" | ")
                        .append(repeat('-', methodWidth))
                        .append(" | ")
                        .append(repeat('-', statusWidth))
                        .append(" | ")
                        .append(repeat('-', nameWidth))
                        .append('\n');
            }
        }
        return builder.toString();
    }

    private String formatDirectorySummary(ExportReport report) {
        StringBuilder builder = new StringBuilder();
        if (report.projectDirectory() != null) {
            builder.append(BrunoHelperBundle.message("export.report.directory.project"))
                    .append(": ")
                    .append(report.projectDirectory())
                    .append('\n');
        }
        if (report.controllerDirectory() != null) {
            builder.append(BrunoHelperBundle.message("export.report.directory.controller"))
                    .append(": ")
                    .append(report.controllerDirectory())
                    .append('\n');
        }
        return builder.toString();
    }

    private int columnWidth(List<Row> rows, java.util.function.Function<Row, String> extractor) {
        int maxWidth = 0;
        for (Row row : rows) {
            maxWidth = Math.max(maxWidth, displayWidth(extractor.apply(row)));
        }
        return maxWidth;
    }

    private String padRight(String value, int targetWidth) {
        String normalized = value == null ? "" : value;
        int padding = Math.max(0, targetWidth - displayWidth(normalized));
        return normalized + repeat(' ', padding);
    }

    private int displayWidth(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            width += current <= 0x7F ? 1 : 2;
        }
        return width;
    }

    private String repeat(char ch, int count) {
        return String.valueOf(ch).repeat(Math.max(0, count));
    }

    private record Row(
            String relativeUrl,
            String methodName,
            String status,
            String endpointName
    ) {
    }
}

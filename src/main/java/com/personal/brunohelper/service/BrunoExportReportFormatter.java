package com.personal.brunohelper.service;

import com.personal.brunohelper.model.ExportEndpointResult;
import com.personal.brunohelper.model.ExportReport;

import java.util.ArrayList;
import java.util.List;

public final class BrunoExportReportFormatter {

    public String format(ExportReport report) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Relative URL", "Method Name", "Export Result", "Endpoint Name"));
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
        builder.append("Bruno Export Result").append('\n');
        builder.append("服务名: ").append(report.serviceName()).append('\n');
        builder.append("类名: ").append(report.className()).append('\n');
        builder.append("类里的接口总数: ").append(report.controllerEndpointCount()).append('\n');
        builder.append("执行导出的接口数量: ").append(report.exportedEndpointCount()).append('\n');
        builder.append("跳过的接口数量: ").append(report.skippedEndpointCount()).append('\n');
        builder.append("实际成功导出的接口数量: ").append(report.succeededEndpointCount()).append('\n');
        builder.append('\n');

        String border = "+"
                + repeat('-', urlWidth + 2) + "+"
                + repeat('-', methodWidth + 2) + "+"
                + repeat('-', statusWidth + 2) + "+"
                + repeat('-', nameWidth + 2) + "+";
        builder.append(border).append('\n');
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            builder.append("| ").append(padRight(row.relativeUrl(), urlWidth))
                    .append(" | ").append(padRight(row.methodName(), methodWidth))
                    .append(" | ").append(padRight(row.status(), statusWidth))
                    .append(" | ").append(padRight(row.endpointName(), nameWidth))
                    .append(" |").append('\n');
            if (index == 0) {
                builder.append(border).append('\n');
            }
        }
        builder.append(border).append('\n');
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

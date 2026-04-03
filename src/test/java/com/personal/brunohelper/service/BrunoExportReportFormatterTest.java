package com.personal.brunohelper.service;

import com.personal.brunohelper.model.ExportEndpointResult;
import com.personal.brunohelper.model.ExportEndpointStatus;
import com.personal.brunohelper.model.ExportReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BrunoExportReportFormatterTest {

    @Test
    void shouldRenderSummaryAndTable() {
        ExportReport report = new ExportReport(
                "sch-order-service",
                "OrderFileController",
                5,
                2,
                1,
                1,
                List.of(
                        new ExportEndpointResult("/order-files/:id", "getById", "查询订单文件", ExportEndpointStatus.SUCCESS, "GET-order-files-id.yml", null),
                        new ExportEndpointResult("/order-files/export", "export", "导出订单文件", ExportEndpointStatus.SKIPPED, "POST-order-files-export.yml", "exists")
                )
        );

        String content = new BrunoExportReportFormatter().format(report);

        assertTrue(content.contains("服务名: sch-order-service"));
        assertTrue(content.contains("类名: OrderFileController"));
        assertTrue(content.contains("类里的接口总数: 5"));
        assertTrue(content.contains("执行导出的接口数量: 2"));
        assertTrue(content.contains("跳过的接口数量: 1"));
        assertTrue(content.contains("实际成功导出的接口数量: 1"));
        assertTrue(content.contains("Bruno Export Result"));
        assertTrue(content.contains("| Relative URL"));
        assertTrue(content.contains("| /order-files/:id"));
        assertTrue(content.contains("查询订单文件"));
        assertTrue(content.contains("SUCCESS"));
        assertTrue(content.contains("SKIPPED"));
        assertTrue(!content.contains("YAML File"));
        assertTrue(!content.contains("GET-order-files-id.yml"));
    }
}

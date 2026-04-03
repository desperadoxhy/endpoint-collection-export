package com.personal.brunohelper.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocCommentUtilTest {

    @Test
    void shouldKeepOnlyFirstLineAndRemoveHtmlTags() {
        String summary = DocCommentUtil.sanitizeSummaryText("<p>导出订单文件</p>\n<p>第二行说明</p>");
        assertEquals("导出订单文件", summary);
    }

    @Test
    void shouldTreatBrAsLineBreakForSummary() {
        String summary = DocCommentUtil.sanitizeSummaryText("导出订单文件<br/>第二行说明");
        assertEquals("导出订单文件", summary);
    }
}

package com.personal.brunohelper.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BrunoExportOptionsTest {

    @Test
    void shouldValidateGlobalBaseOutputDirectory() {
        assertEquals("请输入 Bruno 基础输出目录。", BrunoExportOptions.validateBaseOutputDirectory("", false));
        assertEquals("Bruno 基础输出目录必须使用绝对路径。", BrunoExportOptions.validateBaseOutputDirectory("bruno/output", false));
        assertNull(BrunoExportOptions.validateBaseOutputDirectory("/workspace/bruno-output", false));
    }

    @Test
    void shouldResolveProjectAndControllerDirectories() {
        Path projectDirectory = BrunoExportOptions.resolveProjectDirectory(
                Path.of("/workspace/bruno-output"),
                "demo project"
        );
        assertEquals(
                Path.of("/workspace/bruno-output/demo-project"),
                projectDirectory
        );
        assertEquals(
                Path.of("/workspace/bruno-output/demo-project/OrderFileController"),
                BrunoExportOptions.resolveControllerDirectory(projectDirectory, "OrderFileController")
        );
        assertEquals(
                Path.of("/workspace/workspace.yml"),
                BrunoExportOptions.resolveWorkspaceFile(Path.of("/workspace/bruno-output"))
        );
    }

    @Test
    void shouldTrimControllerSuffixForCollectionName() {
        assertEquals("SaleOrder", BrunoExportOptions.deriveCollectionName("SaleOrderController"));
        assertEquals("HealthCheck", BrunoExportOptions.deriveCollectionName("HealthCheck"));
    }

    @Test
    void shouldSanitizeCollectionDirectoryName() {
        assertEquals(
                "Sale-Order-Export",
                BrunoExportOptions.sanitizeFileSystemName(" Sale / Order : Export ")
        );
        assertEquals("bruno-export", BrunoExportOptions.sanitizeFileSystemName("   "));
        assertEquals("user-list", BrunoExportOptions.sanitizeFileSystemName("user   list"));
    }
}

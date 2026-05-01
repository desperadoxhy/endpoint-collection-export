package com.personal.brunohelper.service;

import com.personal.brunohelper.i18n.BrunoHelperBundle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BrunoExportOptionsTest {

    @Test
    void shouldValidateGlobalBaseOutputDirectory() {
        assertEquals(
                BrunoHelperBundle.message("export.validation.baseOutput.blank"),
                BrunoExportOptions.validateBaseOutputDirectory("", false)
        );
        assertEquals(
                BrunoHelperBundle.message("export.validation.baseOutput.absolute"),
                BrunoExportOptions.validateBaseOutputDirectory("collection/output", false)
        );
        assertNull(BrunoExportOptions.validateBaseOutputDirectory("/workspace/collection-output", false));
    }

    @Test
    void shouldResolveProjectAndControllerDirectories() {
        Path projectDirectory = BrunoExportOptions.resolveProjectDirectory(
                Path.of("/workspace/collection-output"),
                "demo project"
        );
        assertEquals(
                Path.of("/workspace/collection-output/demo-project"),
                projectDirectory
        );
        assertEquals(
                Path.of("/workspace/collection-output/demo-project/SampleController"),
                BrunoExportOptions.resolveControllerDirectory(projectDirectory, "SampleController")
        );
        assertEquals(
                Path.of("/workspace/collection-output/demo-project/SampleController"),
                BrunoExportOptions.resolveControllerDirectory(projectDirectory, "SampleController", null)
        );
        assertEquals(
                Path.of("/workspace/workspace.yml"),
                BrunoExportOptions.resolveWorkspaceFile(Path.of("/workspace/collection-output"))
        );
    }

    @Test
    void shouldResolveControllerDirectoryWithPackageSubdirectory() {
        Path projectDirectory = Path.of("/workspace/collection-output/demo-project");

        // 测试没有 PsiClass 的情况（向后兼容）
        assertEquals(
                Path.of("/workspace/collection-output/demo-project/UserController"),
                BrunoExportOptions.resolveControllerDirectory(projectDirectory, "UserController", null)
        );

        // TODO: 添加实际的 PsiClass 测试用例
        // 需要完整的 IntelliJ Platform 测试环境来创建 PsiClass mock
        // 实际行为：
        // com.example.app.controller.omc.UserController -> demo-project/omc/UserController
        // com.example.app.controller.UserController -> demo-project/UserController
        // com.example.service.UserController -> demo-project/UserController
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
        assertEquals("collection-export", BrunoExportOptions.sanitizeFileSystemName("   "));
        assertEquals("user-list", BrunoExportOptions.sanitizeFileSystemName("user   list"));
    }
}

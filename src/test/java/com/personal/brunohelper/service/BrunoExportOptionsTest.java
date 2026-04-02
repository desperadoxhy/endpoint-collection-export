package com.personal.brunohelper.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrunoExportOptionsTest {

    @Test
    void shouldUseProjectDefaultOutputDirectoryWhenBlank() {
        Path outputDirectory = BrunoExportOptions.resolveOutputDirectory("/workspace/demo", "");

        assertEquals(Path.of("/workspace/demo/bruno"), outputDirectory);
    }

    @Test
    void shouldAcceptBruCommandName() {
        assertTrue(BrunoExportOptions.hasConfiguredBruCliPath("bru"));
        assertEquals("bru", BrunoExportOptions.resolveBruCliPath("bru"));
        assertNull(BrunoExportOptions.validateBruCliPath("bru", false));
    }

    @Test
    void shouldRejectRelativeBruInstallationPath() {
        assertEquals(
                "Bruno CLI 可执行文件路径必须使用绝对路径；如已在 PATH 中，可直接填写 bru。",
                BrunoExportOptions.validateBruCliPath("tools/bru", false)
        );
    }

    @Test
    void shouldResolveExecutableInsideConfiguredDirectory(@TempDir Path tempDir) throws IOException {
        Path bruExecutable = Files.createFile(tempDir.resolve("bru.cmd"));

        assertEquals(bruExecutable.toString(), BrunoExportOptions.resolveBruCliPath(tempDir.toString()));
        assertNull(BrunoExportOptions.validateBruCliPath(tempDir.toString(), false));
    }

    @Test
    void shouldTrimControllerSuffixForCollectionName() {
        assertEquals("SaleOrder", BrunoExportOptions.deriveCollectionName("SaleOrderController"));
        assertEquals("HealthCheck", BrunoExportOptions.deriveCollectionName("HealthCheck"));
    }
}

package com.personal.brunohelper.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BrunoExportOptionsTest {

    @Test
    void shouldUseProjectDefaultOutputDirectoryWhenBlank() {
        Path outputDirectory = BrunoExportOptions.resolveOutputDirectory("/workspace/demo", "");

        assertEquals(Path.of("/workspace/demo/bruno"), outputDirectory);
    }

    @Test
    void shouldTrimControllerSuffixForCollectionName() {
        assertEquals("SaleOrder", BrunoExportOptions.deriveCollectionName("SaleOrderController"));
        assertEquals("HealthCheck", BrunoExportOptions.deriveCollectionName("HealthCheck"));
    }

    @Test
    void shouldResolveCmdFromWindowsPath(@TempDir Path tempDir) throws IOException {
        Path npmBinDirectory = Files.createDirectories(tempDir.resolve("npm-bin"));
        Path bruCommand = Files.createFile(npmBinDirectory.resolve("bru.cmd"));

        String resolved = BrunoExportOptions.resolveCommandOnWindows(
                "bru",
                npmBinDirectory.toString(),
                ".COM;.EXE;.BAT;.CMD"
        );

        assertEquals(bruCommand.toString(), resolved);
    }

    @Test
    void shouldPreferCmdOverShellScriptOnWindowsPath(@TempDir Path tempDir) throws IOException {
        Path npmBinDirectory = Files.createDirectories(tempDir.resolve("npm-bin"));
        Files.createFile(npmBinDirectory.resolve("bru"));
        Path bruCommand = Files.createFile(npmBinDirectory.resolve("bru.cmd"));

        String resolved = BrunoExportOptions.resolveCommandOnWindows(
                "bru",
                npmBinDirectory.toString(),
                ".COM;.EXE;.BAT;.CMD"
        );

        assertEquals(bruCommand.toString(), resolved);
    }

    @Test
    void shouldReturnNullWhenWindowsPathDoesNotContainBru() {
        assertNull(BrunoExportOptions.resolveCommandOnWindows("bru", "", ".COM;.EXE;.BAT;.CMD"));
    }

    @Test
    void shouldUseRelativeOpenApiSourceOnWindows() {
        Path openApiFile = Path.of("/tmp/demo", "OrderFileController.openapi.json");

        assertEquals(
                "OrderFileController.openapi.json",
                BrunoExportOptions.resolveOpenApiSourceArgument(openApiFile, true)
        );
    }

    @Test
    void shouldUseParentDirectoryAsWorkingDirectoryOnWindows() {
        Path openApiFile = Path.of("/tmp/demo", "OrderFileController.openapi.json");

        assertEquals(
                Path.of("/tmp/demo"),
                BrunoExportOptions.resolveBruWorkingDirectory(openApiFile, true)
        );
    }
}

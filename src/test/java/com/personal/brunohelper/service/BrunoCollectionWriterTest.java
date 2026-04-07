package com.personal.brunohelper.service;

import com.intellij.psi.PsiType;
import com.personal.brunohelper.model.ControllerExportModel;
import com.personal.brunohelper.model.EndpointExportModel;
import com.personal.brunohelper.model.EndpointParameterModel;
import com.personal.brunohelper.model.ExportEndpointStatus;
import com.personal.brunohelper.model.ParameterSource;
import com.personal.brunohelper.model.RequestBodyModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrunoCollectionWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateOpenCollectionFilesForGetEndpoint() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("OrderFileController");
        ControllerExportModel model = new ControllerExportModel(
                "OrderFileController",
                "订单文件",
                "导出订单文件接口",
                List.of(new EndpointExportModel(
                        "OrderFileController.getById",
                        "查询订单文件",
                        "根据订单文件ID查询详情",
                        List.of("/order-files/{id}"),
                        Set.of("GET"),
                        List.of(
                                new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiType.INT),
                                new EndpointParameterModel("page", "", false, "1", ParameterSource.REQUEST_PARAM, PsiType.INT),
                                new EndpointParameterModel("X-Trace-Id", "", false, null, ParameterSource.REQUEST_HEADER, PsiType.INT)
                        ),
                        null,
                        PsiType.VOID
                ))
        );

        BrunoCollectionWriter.GenerationResult result = writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals("demo project", result.collectionName());
        assertEquals(projectDirectory, result.projectDirectory());
        assertEquals(controllerDirectory, result.controllerDirectory());
        assertEquals(1, result.createdRequestCount());
        assertEquals(0, result.skippedRequestCount());
        assertEquals(0, result.failedRequestCount());
        assertEquals(1, result.endpointResults().size());
        assertEquals(ExportEndpointStatus.SUCCESS, result.endpointResults().get(0).status());
        assertEquals("/order-files/:id", result.endpointResults().get(0).relativeUrl());
        assertTrue(Files.exists(result.projectDirectory().resolve("opencollection.yml")));
        assertTrue(Files.exists(result.controllerDirectory().resolve("folder.yml")));
        assertTrue(Files.exists(result.controllerDirectory().resolve("GET-order-files-id-OrderFileController.getById.yml")));
        assertTrue(Files.notExists(result.controllerDirectory().resolve("opencollection.yml")));

        String collectionFile = Files.readString(result.projectDirectory().resolve("opencollection.yml"));
        assertTrue(collectionFile.contains("opencollection: 1.0.0"));
        assertTrue(collectionFile.contains("name: \"demo project\""));
        String folderFile = Files.readString(result.controllerDirectory().resolve("folder.yml"));
        assertTrue(folderFile.contains("info:"));
        assertTrue(folderFile.contains("name: \"订单文件\""));

        Path requestFile = result.controllerDirectory().resolve("GET-order-files-id-OrderFileController.getById.yml");
        String requestContent = Files.readString(requestFile);
        assertTrue(requestContent.contains("url: \"{{baseUrl}}/order-files/:id\""));
        assertTrue(requestContent.contains("type: \"path\""));
        assertTrue(requestContent.contains("name: \"page\""));
        assertTrue(requestContent.contains("value: \"1\""));
        assertTrue(requestContent.contains("name: \"X-Trace-Id\""));
        assertTrue(requestContent.contains("docs: |-"));
    }

    @Test
    void shouldGenerateJsonBodyForRequestBodyEndpoint() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("HealthController");
        ControllerExportModel model = new ControllerExportModel(
                "HealthController",
                "健康检查",
                "",
                List.of(new EndpointExportModel(
                        "HealthController.echo",
                        "提交健康检查",
                        "",
                        List.of("/health/echo"),
                        Set.of("POST"),
                        List.of(),
                        new RequestBodyModel(PsiType.INT, true, "", "application/json"),
                        PsiType.VOID
                ))
        );

        BrunoCollectionWriter.GenerationResult result = writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(1, result.createdRequestCount());
        assertEquals(0, result.skippedRequestCount());
        assertEquals(0, result.failedRequestCount());
        Path requestFile = result.controllerDirectory().resolve("POST-health-echo-HealthController.echo.yml");
        String requestContent = Files.readString(requestFile);
        assertTrue(requestContent.contains("method: POST"));
        assertTrue(requestContent.contains("type: \"json\""));
        assertTrue(requestContent.contains("name: \"Content-Type\""));
        assertTrue(requestContent.contains("value: \"application/json\""));
        assertTrue(requestContent.contains("0"));
    }

    @Test
    void shouldKeepExistingProjectOpenCollectionFile() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("HealthController");
        Files.createDirectories(projectDirectory);
        Files.writeString(projectDirectory.resolve("opencollection.yml"), "existing-opencollection", java.nio.charset.StandardCharsets.UTF_8);

        ControllerExportModel model = new ControllerExportModel(
                "HealthController",
                "健康检查",
                "",
                List.of(new EndpointExportModel(
                        "HealthController.echo",
                        "提交健康检查",
                        "",
                        List.of("/health/echo"),
                        Set.of("POST"),
                        List.of(),
                        null,
                        PsiType.VOID
                ))
        );

        writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(
                "existing-opencollection",
                Files.readString(projectDirectory.resolve("opencollection.yml"))
        );
    }

    @Test
    void shouldKeepExistingControllerDirectoryContents() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("OrderFileController");
        Files.createDirectories(controllerDirectory);
        Path existingNote = controllerDirectory.resolve("README.txt");
        Files.writeString(existingNote, "keep-me", java.nio.charset.StandardCharsets.UTF_8);

        ControllerExportModel model = new ControllerExportModel(
                "OrderFileController",
                "订单文件",
                "",
                List.of(new EndpointExportModel(
                        "OrderFileController.getById",
                        "查询订单文件",
                        "",
                        List.of("/order-files/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiType.INT)),
                        null,
                        PsiType.VOID
                ))
        );

        BrunoCollectionWriter.GenerationResult result = writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(1, result.createdRequestCount());
        assertEquals(0, result.failedRequestCount());
        assertEquals("keep-me", Files.readString(existingNote));
        assertTrue(Files.exists(controllerDirectory.resolve("GET-order-files-id-OrderFileController.getById.yml")));
    }

    @Test
    void shouldSkipExistingApiFileWithoutOverwriting() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("OrderFileController");
        Files.createDirectories(controllerDirectory);
        Path existingRequestFile = controllerDirectory.resolve("GET-order-files-id-OrderFileController.getById.yml");
        Files.writeString(existingRequestFile, "existing-request", java.nio.charset.StandardCharsets.UTF_8);

        ControllerExportModel model = new ControllerExportModel(
                "OrderFileController",
                "订单文件",
                "",
                List.of(new EndpointExportModel(
                        "OrderFileController.getById",
                        "查询订单文件",
                        "",
                        List.of("/order-files/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiType.INT)),
                        null,
                        PsiType.VOID
                ))
        );

        BrunoCollectionWriter.GenerationResult result = writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(0, result.createdRequestCount());
        assertEquals(1, result.skippedRequestCount());
        assertEquals(0, result.failedRequestCount());
        assertEquals(ExportEndpointStatus.SKIPPED, result.endpointResults().get(0).status());
        assertEquals("existing-request", Files.readString(existingRequestFile));
    }

    @Test
    void shouldUpdateControllerFolderMetadataName() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("OrderFileController");
        Files.createDirectories(controllerDirectory);
        Files.writeString(controllerDirectory.resolve("folder.yml"), "info:\n  name: \"旧名称\"\n", java.nio.charset.StandardCharsets.UTF_8);

        ControllerExportModel model = new ControllerExportModel(
                "OrderFileController",
                "订单文件",
                "",
                List.of(new EndpointExportModel(
                        "OrderFileController.getById",
                        "查询订单文件",
                        "",
                        List.of("/order-files/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiType.INT)),
                        null,
                        PsiType.VOID
                ))
        );

        writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(
                "info:\n  name: \"订单文件\"\n",
                Files.readString(controllerDirectory.resolve("folder.yml"))
        );
    }

    @Test
    void shouldFallbackToControllerNameWhenSummaryIsBlank() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("OrderFileController");

        ControllerExportModel model = new ControllerExportModel(
                "OrderFileController",
                "",
                "",
                List.of(new EndpointExportModel(
                        "OrderFileController.getById",
                        "查询订单文件",
                        "",
                        List.of("/order-files/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiType.INT)),
                        null,
                        PsiType.VOID
                ))
        );

        writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(
                "info:\n  name: \"OrderFileController\"\n",
                Files.readString(controllerDirectory.resolve("folder.yml"))
        );
    }
}

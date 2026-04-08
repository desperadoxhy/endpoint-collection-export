package com.personal.brunohelper.service;

import com.intellij.psi.PsiTypes;
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
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("SampleController");
        ControllerExportModel model = new ControllerExportModel(
                "SampleController",
                "示例接口",
                "导出示例接口",
                List.of(new EndpointExportModel(
                        "SampleController.getById",
                        "查询示例接口",
                        "根据示例接口ID查询详情",
                        List.of("/samples/{id}"),
                        Set.of("GET"),
                        List.of(
                                new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiTypes.intType()),
                                new EndpointParameterModel("page", "", false, "1", ParameterSource.REQUEST_PARAM, PsiTypes.intType()),
                                new EndpointParameterModel("X-Trace-Id", "", false, null, ParameterSource.REQUEST_HEADER, PsiTypes.intType())
                        ),
                        null,
                        PsiTypes.voidType()
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
        assertEquals("/samples/:id", result.endpointResults().get(0).relativeUrl());
        assertTrue(Files.exists(result.projectDirectory().resolve("opencollection.yml")));
        assertTrue(Files.exists(result.controllerDirectory().resolve("folder.yml")));
        assertTrue(Files.exists(result.controllerDirectory().resolve("GET-samples-id-SampleController.getById.yml")));
        assertTrue(Files.notExists(result.controllerDirectory().resolve("opencollection.yml")));
        assertTrue(Files.exists(tempDir.resolve("workspace.yml")));

        String collectionFile = Files.readString(result.projectDirectory().resolve("opencollection.yml"));
        assertTrue(collectionFile.contains("opencollection: 1.0.0"));
        assertTrue(collectionFile.contains("name: \"demo project\""));
        String workspaceFile = Files.readString(tempDir.resolve("workspace.yml"));
        assertTrue(workspaceFile.contains("type: workspace"));
        assertTrue(workspaceFile.contains("name: \"demo project\""));
        assertTrue(workspaceFile.contains("path: \"collections/demo-project\""));
        String folderFile = Files.readString(result.controllerDirectory().resolve("folder.yml"));
        assertTrue(folderFile.contains("info:"));
        assertTrue(folderFile.contains("name: \"示例接口\""));

        Path requestFile = result.controllerDirectory().resolve("GET-samples-id-SampleController.getById.yml");
        String requestContent = Files.readString(requestFile);
        assertTrue(requestContent.contains("url: \"{{baseUrl}}/samples/:id\""));
        assertTrue(requestContent.contains("type: \"path\""));
        assertTrue(requestContent.contains("name: \"page\""));
        assertTrue(requestContent.contains("value: \"1\""));
        assertTrue(requestContent.contains("name: \"X-Trace-Id\""));
        assertTrue(requestContent.contains("docs: |-"));
    }

    @Test
    void shouldGenerateJsonBodyForRequestBodyEndpoint() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
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
                        new RequestBodyModel(PsiTypes.intType(), true, "", "application/json"),
                        PsiTypes.voidType()
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
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
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
                        PsiTypes.voidType()
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
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("SampleController");
        Files.createDirectories(controllerDirectory);
        Path existingNote = controllerDirectory.resolve("README.txt");
        Files.writeString(existingNote, "keep-me", java.nio.charset.StandardCharsets.UTF_8);

        ControllerExportModel model = new ControllerExportModel(
                "SampleController",
                "示例接口",
                "",
                List.of(new EndpointExportModel(
                        "SampleController.getById",
                        "查询示例接口",
                        "",
                        List.of("/samples/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiTypes.intType())),
                        null,
                        PsiTypes.voidType()
                ))
        );

        BrunoCollectionWriter.GenerationResult result = writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(1, result.createdRequestCount());
        assertEquals(0, result.failedRequestCount());
        assertEquals("keep-me", Files.readString(existingNote));
        assertTrue(Files.exists(controllerDirectory.resolve("GET-samples-id-SampleController.getById.yml")));
    }

    @Test
    void shouldSkipExistingApiFileWithoutOverwriting() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("SampleController");
        Files.createDirectories(controllerDirectory);
        Path existingRequestFile = controllerDirectory.resolve("GET-samples-id-SampleController.getById.yml");
        Files.writeString(existingRequestFile, "existing-request", java.nio.charset.StandardCharsets.UTF_8);

        ControllerExportModel model = new ControllerExportModel(
                "SampleController",
                "示例接口",
                "",
                List.of(new EndpointExportModel(
                        "SampleController.getById",
                        "查询示例接口",
                        "",
                        List.of("/samples/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiTypes.intType())),
                        null,
                        PsiTypes.voidType()
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
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("SampleController");
        Files.createDirectories(controllerDirectory);
        Files.writeString(controllerDirectory.resolve("folder.yml"), "info:\n  name: \"旧名称\"\n", java.nio.charset.StandardCharsets.UTF_8);

        ControllerExportModel model = new ControllerExportModel(
                "SampleController",
                "示例接口",
                "",
                List.of(new EndpointExportModel(
                        "SampleController.getById",
                        "查询示例接口",
                        "",
                        List.of("/samples/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiTypes.intType())),
                        null,
                        PsiTypes.voidType()
                ))
        );

        writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(
                "info:\n  name: \"示例接口\"\n",
                Files.readString(controllerDirectory.resolve("folder.yml"))
        );
    }

    @Test
    void shouldFallbackToControllerNameWhenSummaryIsBlank() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("SampleController");

        ControllerExportModel model = new ControllerExportModel(
                "SampleController",
                "",
                "",
                List.of(new EndpointExportModel(
                        "SampleController.getById",
                        "查询示例接口",
                        "",
                        List.of("/samples/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiTypes.intType())),
                        null,
                        PsiTypes.voidType()
                ))
        );

        writer.writeCollection(model, "demo project", projectDirectory, controllerDirectory);

        assertEquals(
                "info:\n  name: \"SampleController\"\n",
                Files.readString(controllerDirectory.resolve("folder.yml"))
        );
    }

    @Test
    void shouldAppendProjectCollectionToExistingWorkspace() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("SampleController");
        Files.writeString(
                tempDir.resolve("workspace.yml"),
                """
                        opencollection: 1.0.0
                        info:
                          name: "工作"
                          type: workspace

                        collections:
                          - name: "demo-service"
                            path: "collections/demo-service"

                        specs:

                        docs: ''

                        activeEnvironmentUid: 15kgepb00000000000000
                        """,
                java.nio.charset.StandardCharsets.UTF_8
        );

        ControllerExportModel model = new ControllerExportModel(
                "SampleController",
                "示例接口",
                "",
                List.of(new EndpointExportModel(
                        "SampleController.getById",
                        "查询示例接口",
                        "",
                        List.of("/samples/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiTypes.intType())),
                        null,
                        PsiTypes.voidType()
                ))
        );

        writer.writeCollection(model, "demo-project", projectDirectory, controllerDirectory);

        String workspaceFile = Files.readString(tempDir.resolve("workspace.yml"));
        assertTrue(workspaceFile.contains("name: \"工作\""));
        assertTrue(workspaceFile.contains("name: \"demo-service\""));
        assertTrue(workspaceFile.contains("path: \"collections/demo-service\""));
        assertTrue(workspaceFile.contains("name: \"demo-project\""));
        assertTrue(workspaceFile.contains("path: \"collections/demo-project\""));
        assertTrue(workspaceFile.contains("activeEnvironmentUid: 15kgepb00000000000000"));
    }

    @Test
    void shouldNotDuplicateExistingWorkspaceCollection() throws IOException {
        BrunoCollectionWriter writer = new BrunoCollectionWriter();
        Path projectDirectory = tempDir.resolve("collections").resolve("demo-project");
        Path controllerDirectory = projectDirectory.resolve("SampleController");
        Files.writeString(
                tempDir.resolve("workspace.yml"),
                """
                        opencollection: 1.0.0
                        info:
                          name: "工作"
                          type: workspace

                        collections:
                          - name: "demo-project"
                            path: "collections/demo-project"

                        specs:

                        docs: ''
                        """,
                java.nio.charset.StandardCharsets.UTF_8
        );

        ControllerExportModel model = new ControllerExportModel(
                "SampleController",
                "示例接口",
                "",
                List.of(new EndpointExportModel(
                        "SampleController.getById",
                        "查询示例接口",
                        "",
                        List.of("/samples/{id}"),
                        Set.of("GET"),
                        List.of(new EndpointParameterModel("id", "", true, null, ParameterSource.PATH_VARIABLE, PsiTypes.intType())),
                        null,
                        PsiTypes.voidType()
                ))
        );

        writer.writeCollection(model, "demo-project", projectDirectory, controllerDirectory);

        String workspaceFile = Files.readString(tempDir.resolve("workspace.yml"));
        assertEquals(1, workspaceFile.split("path: \"collections/demo-project\"", -1).length - 1);
    }
}

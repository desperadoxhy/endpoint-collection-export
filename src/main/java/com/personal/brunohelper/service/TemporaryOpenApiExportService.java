package com.personal.brunohelper.service;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.PsiClass;
import com.personal.brunohelper.model.ControllerExportModel;
import com.personal.brunohelper.model.ExportOutcome;
import com.personal.brunohelper.model.GeneratedOpenApiDocument;
import com.personal.brunohelper.openapi.OpenApiDocumentBuilder;
import com.personal.brunohelper.parser.SpringControllerParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TemporaryOpenApiExportService implements ControllerExportService {

    private final Project project;
    private final SpringControllerParser parser = new SpringControllerParser();
    private final OpenApiDocumentBuilder openApiDocumentBuilder = new OpenApiDocumentBuilder();

    public TemporaryOpenApiExportService(Project project) {
        this.project = project;
    }

    @Override
    public ExportOutcome export(SmartPsiElementPointer<PsiClass> controllerPointer) {
        GeneratedOpenApiDocument openApiDocument = ReadAction.compute(() -> buildDocument(controllerPointer));
        if (openApiDocument == null) {
            return ExportOutcome.failure("当前 controller 已失效，无法继续导出。");
        }
        if (openApiDocument.getEndpointCount() == 0) {
            return ExportOutcome.failure("未在当前 controller 中识别到 Spring MVC 接口。");
        }

        try {
            Path openApiFile = Files.createTempFile(sanitizeFileName(openApiDocument.getControllerName()) + "-", ".openapi.json");
            Files.writeString(openApiFile, openApiDocument.getJson(), StandardCharsets.UTF_8);
            return ExportOutcome.success("已生成临时 OpenAPI 文件（" + openApiDocument.getEndpointCount() + " 个接口）: " + openApiFile);
        } catch (IOException exception) {
            return ExportOutcome.failure("写入临时 OpenAPI 文件失败: " + exception.getMessage());
        }
    }

    public SmartPsiElementPointer<PsiClass> createPointer(PsiClass controllerClass) {
        return SmartPointerManager.getInstance(project).createSmartPsiElementPointer(controllerClass);
    }

    private GeneratedOpenApiDocument buildDocument(SmartPsiElementPointer<PsiClass> controllerPointer) {
        PsiClass controllerClass = controllerPointer.getElement();
        if (controllerClass == null || !controllerClass.isValid()) {
            return null;
        }
        ControllerExportModel exportModel = parser.parse(controllerClass);
        return openApiDocumentBuilder.build(exportModel);
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

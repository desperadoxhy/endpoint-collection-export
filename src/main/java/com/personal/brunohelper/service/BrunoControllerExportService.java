package com.personal.brunohelper.service;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.personal.brunohelper.model.ControllerExportModel;
import com.personal.brunohelper.model.ExportOutcome;
import com.personal.brunohelper.model.ExportReport;
import com.personal.brunohelper.parser.SpringControllerParser;
import com.personal.brunohelper.settings.BrunoHelperSettingsState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BrunoControllerExportService implements ControllerExportService {

    private final Project project;
    private final SpringControllerParser parser = new SpringControllerParser();
    private final BrunoCollectionWriter collectionWriter = new BrunoCollectionWriter();

    public BrunoControllerExportService(Project project) {
        this.project = project;
    }

    @Override
    public ExportOutcome export(
            SmartPsiElementPointer<PsiClass> controllerPointer,
            @Nullable SmartPsiElementPointer<PsiMethod> methodPointer
    ) {
        ParsedControllerModels parsedModels = ReadAction.compute(() -> buildModels(controllerPointer, methodPointer));
        if (parsedModels == null) {
            return ExportOutcome.failure("当前 controller 已失效，无法继续导出。");
        }
        ControllerExportModel controllerModel = parsedModels.controllerModel();
        ControllerExportModel exportModel = parsedModels.exportModel();
        if (exportModel.getEndpoints().isEmpty()) {
            return ExportOutcome.failure("未在当前 controller 中识别到 Spring MVC 接口。");
        }

        BrunoHelperSettingsState settings = BrunoHelperSettingsState.getInstance();
        Path baseOutputDirectory = BrunoExportOptions.resolveBaseOutputDirectory(settings.getCollectionOutputDirectory());
        Path projectDirectory = null;
        Path controllerDirectory = null;
        Path workspaceFile = BrunoExportOptions.resolveWorkspaceFile(baseOutputDirectory);
        try {
            projectDirectory = BrunoExportOptions.resolveProjectDirectory(baseOutputDirectory, project.getName());
            controllerDirectory = BrunoExportOptions.resolveControllerDirectory(projectDirectory, exportModel.getControllerName());
            Files.createDirectories(projectDirectory);
        } catch (IOException exception) {
            return ExportOutcome.failure(
                    "创建 Bruno 输出目录失败: " + exception.getMessage(),
                    emptyReport(controllerModel, projectDirectory, controllerDirectory)
            );
        }

        Path finalProjectDirectory = projectDirectory;
        Path finalControllerDirectory = controllerDirectory;
        Path finalWorkspaceFile = workspaceFile;
        BrunoCollectionWriter.PreparedCollection preparedCollection = ReadAction.compute(() ->
                collectionWriter.prepareCollection(
                        exportModel,
                        project.getName(),
                        finalProjectDirectory,
                        finalControllerDirectory,
                        finalWorkspaceFile
                )
        );
        if (preparedCollection == null) {
            return ExportOutcome.failure("当前 controller 已失效，无法继续导出。", emptyReport(controllerModel));
        }

        try {
            BrunoCollectionWriter.GenerationResult result = collectionWriter.writePreparedCollection(preparedCollection);
            ExportReport report = buildReport(controllerModel, result);
            String message = "已更新 Bruno 项目 `" + result.collectionName()
                    + "`，项目目录: " + result.projectDirectory()
                    + "，controller目录: " + result.controllerDirectory()
                    + "，新增 " + result.createdRequestCount() + " 个接口文件，跳过 " + result.skippedRequestCount()
                    + " 个已存在文件，失败 " + result.failedRequestCount() + " 个接口文件。";
            if (result.failedRequestCount() > 0) {
                return ExportOutcome.failure(message, report);
            }
            return ExportOutcome.success(message, report);
        } catch (IOException exception) {
            return ExportOutcome.failure(
                    "生成 Bruno Collection 文件失败: " + exception.getMessage(),
                    emptyReport(controllerModel, finalProjectDirectory, finalControllerDirectory)
            );
        }
    }

    public SmartPsiElementPointer<PsiClass> createPointer(PsiClass controllerClass) {
        return SmartPointerManager.getInstance(project).createSmartPsiElementPointer(controllerClass);
    }

    public @Nullable SmartPsiElementPointer<PsiMethod> createPointer(@Nullable PsiMethod method) {
        if (method == null) {
            return null;
        }
        return SmartPointerManager.getInstance(project).createSmartPsiElementPointer(method);
    }

    private ParsedControllerModels buildModels(
            SmartPsiElementPointer<PsiClass> controllerPointer,
            @Nullable SmartPsiElementPointer<PsiMethod> methodPointer
    ) {
        PsiClass controllerClass = controllerPointer.getElement();
        if (controllerClass == null || !controllerClass.isValid()) {
            return null;
        }
        PsiMethod method = methodPointer == null ? null : methodPointer.getElement();
        if (methodPointer != null && (method == null || !method.isValid())) {
            return null;
        }
        ControllerExportModel controllerModel = parser.parse(controllerClass);
        ControllerExportModel exportModel = parser.parse(controllerClass, method);
        return new ParsedControllerModels(controllerModel, exportModel);
    }

    private ExportReport buildReport(
            ControllerExportModel controllerModel,
            BrunoCollectionWriter.GenerationResult generationResult
    ) {
        return new ExportReport(
                project.getName(),
                controllerModel.getControllerName(),
                generationResult.projectDirectory(),
                generationResult.controllerDirectory(),
                controllerModel.getEndpoints().size(),
                generationResult.endpointResults().size(),
                generationResult.skippedRequestCount(),
                generationResult.createdRequestCount(),
                generationResult.failedRequestCount(),
                generationResult.endpointResults()
        );
    }

    private ExportReport emptyReport(ControllerExportModel controllerModel) {
        return emptyReport(controllerModel, null, null);
    }

    private ExportReport emptyReport(
            ControllerExportModel controllerModel,
            @Nullable Path projectDirectory,
            @Nullable Path controllerDirectory
    ) {
        return new ExportReport(
                project.getName(),
                controllerModel.getControllerName(),
                projectDirectory,
                controllerDirectory,
                controllerModel.getEndpoints().size(),
                0,
                0,
                0,
                0,
                java.util.List.of()
        );
    }

    private record ParsedControllerModels(
            ControllerExportModel controllerModel,
            ControllerExportModel exportModel
    ) {
    }
}

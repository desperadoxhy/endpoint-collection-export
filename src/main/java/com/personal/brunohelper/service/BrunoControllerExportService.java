package com.personal.brunohelper.service;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.personal.brunohelper.model.ControllerExportModel;
import com.personal.brunohelper.model.ExportOutcome;
import com.personal.brunohelper.model.GeneratedOpenApiDocument;
import com.personal.brunohelper.openapi.OpenApiDocumentBuilder;
import com.personal.brunohelper.parser.SpringControllerParser;
import com.personal.brunohelper.settings.BrunoHelperSettingsState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BrunoControllerExportService implements ControllerExportService {

    private static final long CLI_TIMEOUT_SECONDS = 60L;

    private final Project project;
    private final SpringControllerParser parser = new SpringControllerParser();
    private final OpenApiDocumentBuilder openApiDocumentBuilder = new OpenApiDocumentBuilder();

    public BrunoControllerExportService(Project project) {
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

        Path openApiFile;
        try {
            openApiFile = writeTemporaryOpenApi(openApiDocument);
        } catch (IOException exception) {
            return ExportOutcome.failure("写入临时 OpenAPI 文件失败: " + exception.getMessage());
        }

        BrunoHelperSettingsState settings = BrunoHelperSettingsState.getInstance();
        Path outputDirectory;
        try {
            outputDirectory = resolveOutputDirectory(settings);
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            cleanup(openApiFile);
            return ExportOutcome.failure("创建 Bruno 输出目录失败: " + exception.getMessage());
        }

        String collectionName = deriveCollectionName(openApiDocument.getControllerName());

        ProcessResult processResult;
        try {
            processResult = runBruImport(openApiFile, outputDirectory, collectionName);
        } catch (IOException exception) {
            String suffix = maybeKeepTemporaryFile(openApiFile, settings, true);
            return ExportOutcome.failure("执行 Bruno CLI 失败: " + exception.getMessage() + suffix);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            String suffix = maybeKeepTemporaryFile(openApiFile, settings, true);
            return ExportOutcome.failure("等待 Bruno CLI 结果时被中断。" + suffix);
        }

        if (!processResult.success()) {
            String detail = processResult.output().isBlank() ? "" : " CLI 输出: " + processResult.output();
            String suffix = maybeKeepTemporaryFile(openApiFile, settings, true);
            return ExportOutcome.failure("Bruno CLI 导入失败，退出码 " + processResult.exitCode() + "。" + detail + suffix);
        }

        cleanup(openApiFile);
        return ExportOutcome.success("已导入 Bruno Collection `" + collectionName + "` 到目录: " + outputDirectory);
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

    private Path writeTemporaryOpenApi(GeneratedOpenApiDocument openApiDocument) throws IOException {
        Path openApiFile = Files.createTempFile(sanitizeFileName(openApiDocument.getControllerName()) + "-", ".openapi.json");
        Files.writeString(openApiFile, openApiDocument.getJson(), StandardCharsets.UTF_8);
        return openApiFile;
    }

    private Path resolveOutputDirectory(BrunoHelperSettingsState settings) {
        return BrunoExportOptions.resolveOutputDirectory(project.getBasePath(), settings.getCollectionOutputDirectory());
    }

    private ProcessResult runBruImport(Path openApiFile, Path outputDirectory, String collectionName)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(BrunoExportOptions.resolveBruCommand());
        command.add("import");
        command.add("openapi");
        command.add("--source");
        command.add(BrunoExportOptions.resolveOpenApiSourceArgument(openApiFile));
        command.add("--output");
        command.add(outputDirectory.toString());
        command.add("--collection-name");
        command.add(collectionName);

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .redirectErrorStream(true);
        Path workingDirectory = BrunoExportOptions.resolveBruWorkingDirectory(openApiFile);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }

        Process process = processBuilder.start();

        boolean finished = process.waitFor(CLI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new ProcessResult(false, -1, "Bruno CLI 执行超时。");
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        return new ProcessResult(process.exitValue() == 0, process.exitValue(), output);
    }

    private String deriveCollectionName(String controllerName) {
        return BrunoExportOptions.deriveCollectionName(controllerName);
    }

    private String maybeKeepTemporaryFile(Path openApiFile, BrunoHelperSettingsState settings, boolean failed) {
        if (failed && settings.isKeepTemporaryOpenApiFile()) {
            return " 临时 OpenAPI 已保留: " + openApiFile;
        }
        cleanup(openApiFile);
        return "";
    }

    private void cleanup(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private record ProcessResult(boolean success, int exitCode, String output) {
    }
}

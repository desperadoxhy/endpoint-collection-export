package com.personal.brunohelper.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPsiElementPointer;
import com.personal.brunohelper.context.ControllerContextResolver;
import com.personal.brunohelper.context.ControllerContextResolver.ExportTarget;
import com.personal.brunohelper.model.ExportOutcome;
import com.personal.brunohelper.notification.BrunoHelperNotifier;
import com.personal.brunohelper.service.BrunoControllerExportService;
import com.personal.brunohelper.service.BrunoExportOptions;
import com.personal.brunohelper.service.BrunoExportResultConsole;
import com.personal.brunohelper.settings.BrunoHelperSettingsState;
import com.personal.brunohelper.settings.BrunoOutputDirectoryDialog;
import org.jetbrains.annotations.NotNull;

public final class ExportControllerToBrunoAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        ExportTarget target = ControllerContextResolver.resolveTarget(event);
        event.getPresentation().setEnabledAndVisible(target != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        ExportTarget target = ControllerContextResolver.resolveTarget(event);
        if (project == null || target == null) {
            return;
        }
        if (!ensureOutputDirectoryConfigured(project)) {
            BrunoHelperNotifier.warn(project, "已取消导出，未完成 Bruno 输出目录配置。");
            return;
        }
        BrunoControllerExportService exportService = new BrunoControllerExportService(project);
        SmartPsiElementPointer<PsiClass> controllerPointer = exportService.createPointer(target.controllerClass());
        SmartPsiElementPointer<PsiMethod> methodPointer = exportService.createPointer(target.targetMethod());

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "导出到 Bruno", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ExportOutcome outcome = exportService.export(controllerPointer, methodPointer);
                ApplicationManager.getApplication().invokeLater(() -> {
                    new BrunoExportResultConsole().show(project, outcome);
                    if (outcome.isSuccess()) {
                        BrunoHelperNotifier.info(project, outcome.getMessage());
                    } else {
                        BrunoHelperNotifier.warn(project, outcome.getMessage());
                    }
                });
            }
        });
    }

    private boolean ensureOutputDirectoryConfigured(Project project) {
        BrunoHelperSettingsState settings = BrunoHelperSettingsState.getInstance();
        String configuredDirectory = settings.getCollectionOutputDirectory();
        String validationError = BrunoExportOptions.validateBaseOutputDirectory(configuredDirectory, false);
        if (validationError == null) {
            return true;
        }

        BrunoOutputDirectoryDialog dialog = new BrunoOutputDirectoryDialog(project, configuredDirectory);
        if (!dialog.showAndGet()) {
            return false;
        }

        settings.setCollectionOutputDirectory(dialog.getOutputDirectory());
        return true;
    }
}

package com.personal.brunohelper.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.SmartPsiElementPointer;
import com.personal.brunohelper.context.ControllerContextResolver;
import com.personal.brunohelper.model.ExportOutcome;
import com.personal.brunohelper.notification.BrunoHelperNotifier;
import com.personal.brunohelper.service.BrunoExportOptions;
import com.personal.brunohelper.service.BrunoControllerExportService;
import com.personal.brunohelper.settings.BrunoHelperSettingsState;
import com.personal.brunohelper.settings.BrunoCliCommandDialog;
import org.jetbrains.annotations.NotNull;

public final class ExportControllerToBrunoAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        PsiClass controllerClass = ControllerContextResolver.resolveController(event);
        event.getPresentation().setEnabledAndVisible(controllerClass != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        PsiClass controllerClass = ControllerContextResolver.resolveController(event);
        if (project == null || controllerClass == null) {
            return;
        }
        BrunoControllerExportService exportService = new BrunoControllerExportService(project);
        if (!ensureBrunoCliPathConfigured(project)) {
            BrunoHelperNotifier.warn(project, "已取消导出，未完成 Bruno CLI 配置。");
            return;
        }
        SmartPsiElementPointer<PsiClass> controllerPointer = exportService.createPointer(controllerClass);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "导出到 Bruno", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ExportOutcome outcome = exportService.export(controllerPointer);
                if (outcome.isSuccess()) {
                    BrunoHelperNotifier.info(project, outcome.getMessage());
                } else {
                    BrunoHelperNotifier.warn(project, outcome.getMessage());
                }
            }
        });
    }

    private boolean ensureBrunoCliPathConfigured(Project project) {
        BrunoHelperSettingsState settings = BrunoHelperSettingsState.getInstance();
        String configuredPath = settings.getBruCliPath();
        String validationError = BrunoExportOptions.validateBruCliPath(configuredPath, true);
        if (BrunoExportOptions.hasConfiguredBruCliPath(configuredPath) && validationError == null) {
            return true;
        }

        BrunoCliCommandDialog dialog = new BrunoCliCommandDialog(project, configuredPath);
        if (!dialog.showAndGet()) {
            return false;
        }

        settings.setBruCliPath(dialog.getBruCliCommand());
        return true;
    }
}

package com.personal.brunohelper.service;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.personal.brunohelper.model.ExportOutcome;
import com.personal.brunohelper.model.ExportReport;

public final class BrunoExportResultConsole {

    private final BrunoExportReportFormatter formatter = new BrunoExportReportFormatter();

    public void show(Project project, ExportOutcome outcome) {
        ExportReport report = outcome.getReport();
        if (report == null) {
            return;
        }

        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        consoleView.print(outcome.getMessage() + "\n\n", outcome.isSuccess()
                ? ConsoleViewContentType.SYSTEM_OUTPUT
                : ConsoleViewContentType.ERROR_OUTPUT);
        consoleView.print(formatter.format(report), ConsoleViewContentType.NORMAL_OUTPUT);
        consoleView.requestScrollingToEnd();

        RunContentDescriptor descriptor = new RunContentDescriptor(
                consoleView,
                null,
                consoleView.getComponent(),
                "Bruno 导出结果 - " + report.className(),
                null,
                null,
                consoleView.createConsoleActions()
        );
        descriptor.setActivateToolWindowWhenAdded(true);
        descriptor.setSelectContentWhenAdded(true);
        ExecutionManager.getInstance(project)
                .getContentManager()
                .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
    }
}

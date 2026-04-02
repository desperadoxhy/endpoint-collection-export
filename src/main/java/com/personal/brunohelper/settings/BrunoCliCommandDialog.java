package com.personal.brunohelper.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.personal.brunohelper.service.BrunoExportOptions;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class BrunoCliCommandDialog extends DialogWrapper {

    private final TextFieldWithBrowseButton cliCommandField = new TextFieldWithBrowseButton();

    public BrunoCliCommandDialog(@Nullable Project project, @Nullable String initialPath) {
        super(project);
        setTitle("配置 Bruno CLI");
        cliCommandField.setText(initialPath == null ? "" : initialPath.trim());
        cliCommandField.addBrowseFolderListener(
                "选择 Bruno CLI 可执行文件",
                "可直接输入命令名 bru，或浏览选择 Bruno CLI 可执行文件。",
                project,
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        );
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 8, 8);
        panel.add(new JLabel("Bruno CLI 命令"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 8, 0);
        panel.add(cliCommandField, constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(new JLabel("可填写 bru，或浏览选择 Bruno CLI 可执行文件。该配置为全局配置，对所有项目生效。"), constraints);
        return panel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String error = BrunoExportOptions.validateBruCliPath(cliCommandField.getText(), false);
        return error == null ? null : new ValidationInfo(error, cliCommandField.getTextField());
    }

    public String getBruCliCommand() {
        return cliCommandField.getText().trim();
    }
}

package com.personal.brunohelper.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.personal.brunohelper.i18n.BrunoHelperBundle;
import com.personal.brunohelper.service.BrunoExportOptions;
import com.personal.brunohelper.util.FieldBlacklistUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public final class BrunoHelperConfigurable implements Configurable {

    private JPanel panel;
    private TextFieldWithBrowseButton outputDirectoryField;
    private JTextArea fieldBlacklistArea;
    private JButton presetButton;
    private JButton clearButton;

    @Override
    public @Nls String getDisplayName() {
        return BrunoHelperBundle.message("settings.display.name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            panel = new JPanel(new GridBagLayout());
            outputDirectoryField = new TextFieldWithBrowseButton();
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle(BrunoHelperBundle.message("settings.output.directory.chooser.title"))
                    .withDescription(BrunoHelperBundle.message("settings.output.directory.chooser.description"));
            outputDirectoryField.addBrowseFolderListener(new TextBrowseFolderListener(descriptor));

            fieldBlacklistArea = new JTextArea(6, 40);
            fieldBlacklistArea.setFont(new JLabel().getFont());
            JScrollPane blacklistScrollPane = new JScrollPane(fieldBlacklistArea);

            presetButton = new JButton(BrunoHelperBundle.message("settings.field.blacklist.preset"));
            clearButton = new JButton(BrunoHelperBundle.message("settings.field.blacklist.clear"));

            ActionListener presetListener = e -> {
                List<String> current = getBlacklistFromArea();
                List<String> combined = new ArrayList<>(current);
                for (String preset : FieldBlacklistUtil.PRESET_BLACKLIST) {
                    if (!combined.contains(preset)) {
                        combined.add(preset);
                    }
                }
                setBlacklistToArea(combined);
            };
            ActionListener clearListener = e -> {
                fieldBlacklistArea.setText("");
            };

            presetButton.addActionListener(presetListener);
            clearButton.addActionListener(clearListener);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 0, 8, 8);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;

            constraints.gridx = 0;
            constraints.gridy = row;
            constraints.weightx = 0;
            panel.add(new JLabel(BrunoHelperBundle.message("settings.output.directory.label")), constraints);

            constraints.gridx = 1;
            constraints.weightx = 1;
            panel.add(outputDirectoryField, constraints);

            row++;

            constraints.gridx = 1;
            constraints.gridy = row;
            constraints.weightx = 1;
            constraints.insets = new Insets(0, 0, 16, 0);
            panel.add(new JLabel(BrunoHelperBundle.message("settings.output.directory.help")), constraints);

            row++;

            constraints.gridx = 0;
            constraints.gridy = row;
            constraints.weightx = 0;
            constraints.gridheight = 3;
            constraints.insets = new Insets(0, 0, 8, 8);
            panel.add(new JLabel(BrunoHelperBundle.message("settings.field.blacklist.label")), constraints);

            constraints.gridx = 1;
            constraints.gridheight = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.insets = new Insets(0, 0, 4, 0);
            panel.add(blacklistScrollPane, constraints);

            row++;

            constraints.gridx = 1;
            constraints.gridy = row;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(new JLabel(BrunoHelperBundle.message("settings.field.blacklist.help")), constraints);

            row++;

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            buttonPanel.add(presetButton);
            buttonPanel.add(Box.createHorizontalStrut(8));
            buttonPanel.add(clearButton);

            constraints.gridx = 1;
            constraints.gridy = row;
            constraints.weightx = 1;
            panel.add(buttonPanel, constraints);
        }

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        BrunoHelperSettingsState settings = BrunoHelperSettingsState.getInstance();
        boolean directoryModified = !outputDirectoryField.getText().trim().equals(settings.getCollectionOutputDirectory());
        boolean blacklistModified = !getBlacklistFromArea().equals(settings.getFieldBlacklistPatterns());
        return directoryModified || blacklistModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        String outputDirectory = outputDirectoryField.getText().trim();
        String validationError = BrunoExportOptions.validateBaseOutputDirectory(outputDirectory, false);
        if (validationError != null) {
            throw new ConfigurationException(validationError);
        }

        List<String> blacklist = getBlacklistFromArea();
        List<String> patternErrors = FieldBlacklistUtil.validatePatterns(blacklist);
        if (!patternErrors.isEmpty()) {
            throw new ConfigurationException(String.join("\n", patternErrors));
        }

        BrunoHelperSettingsState settings = BrunoHelperSettingsState.getInstance();
        settings.setCollectionOutputDirectory(outputDirectory);
        settings.setFieldBlacklistPatterns(blacklist);
    }

    @Override
    public void reset() {
        BrunoHelperSettingsState settings = BrunoHelperSettingsState.getInstance();
        if (outputDirectoryField != null) {
            outputDirectoryField.setText(settings.getCollectionOutputDirectory());
        }
        if (fieldBlacklistArea != null) {
            setBlacklistToArea(settings.getFieldBlacklistPatterns());
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        outputDirectoryField = null;
        fieldBlacklistArea = null;
        presetButton = null;
        clearButton = null;
    }

    private List<String> getBlacklistFromArea() {
        if (fieldBlacklistArea == null) {
            return new ArrayList<>();
        }
        String text = fieldBlacklistArea.getText();
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> patterns = new ArrayList<>();
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                patterns.add(trimmed);
            }
        }
        return patterns;
    }

    private void setBlacklistToArea(List<String> patterns) {
        if (fieldBlacklistArea == null) {
            return;
        }
        if (patterns == null || patterns.isEmpty()) {
            fieldBlacklistArea.setText("");
        } else {
            fieldBlacklistArea.setText(String.join("\n", patterns));
        }
    }
}

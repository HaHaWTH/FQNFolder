package io.wdsj.fqnfolder.settings;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PluginSettingsComponent {

    private final JPanel mainPanel;
    private final JBTextField foldingThresholdField;
    private final JBCheckBox enabledCheckBox;
    private final JBLabel errorLabel;

    public PluginSettingsComponent() {
        foldingThresholdField = new JBTextField();
        foldingThresholdField.setColumns(10);

        enabledCheckBox = new JBCheckBox("Enable qualified name folding");

        errorLabel = new JBLabel();
        errorLabel.setForeground(JBUI.CurrentTheme.Label.errorForeground());
        errorLabel.setVisible(false);

        foldingThresholdField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateInput();
            }
        });

        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(enabledCheckBox)
                .addVerticalGap(10)
                .addLabeledComponent(new JBLabel("Folding threshold (characters):"), foldingThresholdField, 1, false)
                .addComponent(errorLabel)
                .addComponentToRightColumn(new JBLabel("Names longer than this will be folded"))
                .addVerticalGap(10)
                .addComponent(new JBLabel("<html><body>" +
                        "<b>Examples:</b><br>" +
                        "• java.util.List → List<br>" +
                        "• java.awt.List vs java.util.List → awt.List vs util.List<br>" +
                        "• java.lang.System.out → System.out" +
                        "</body></html>"))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private void validateInput() {
        String text = foldingThresholdField.getText().trim();
        if (text.isEmpty()) {
            showError("Threshold cannot be empty");
            return;
        }

        try {
            int value = Integer.parseInt(text);
            if (value < 1) {
                showError("Threshold must be at least 1");
            } else if (value > 200) {
                showError("Threshold cannot exceed 200");
            } else {
                hideError();
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return foldingThresholdField;
    }

    public int getFoldingThreshold() {
        try {
            String text = foldingThresholdField.getText().trim();
            int value = Integer.parseInt(text);
            return Math.max(1, Math.min(200, value));
        } catch (NumberFormatException e) {
            return 16;
        }
    }

    public void setFoldingThreshold(int threshold) {
        foldingThresholdField.setText(String.valueOf(threshold));
    }

    public boolean isEnabled() {
        return enabledCheckBox.isSelected();
    }

    public void setEnabled(boolean enabled) {
        enabledCheckBox.setSelected(enabled);
    }

    public ValidationInfo validate() {
        String text = foldingThresholdField.getText().trim();
        if (text.isEmpty()) {
            return new ValidationInfo("Threshold cannot be empty", foldingThresholdField);
        }

        try {
            int value = Integer.parseInt(text);
            if (value < 1) {
                return new ValidationInfo("Threshold must be at least 1", foldingThresholdField);
            } else if (value > 200) {
                return new ValidationInfo("Threshold cannot exceed 200", foldingThresholdField);
            }
        } catch (NumberFormatException e) {
            return new ValidationInfo("Please enter a valid number", foldingThresholdField);
        }

        return null;
    }
}
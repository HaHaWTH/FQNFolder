package io.wdsj.fqnfolder.settings;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PluginSettingsConfigurable implements Configurable {

    private PluginSettingsComponent settingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Qualified Name Folder";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        settingsComponent = new PluginSettingsComponent();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        PluginSettings settings = PluginSettings.getInstance();
        return settingsComponent.getFoldingThreshold() != settings.getFoldingThreshold() ||
                settingsComponent.isEnabled() != settings.isEnabled();
    }

    @Override
    public void apply() {
        PluginSettings settings = PluginSettings.getInstance();

        boolean wasEnabled = settings.isEnabled();
        int oldThreshold = settings.getFoldingThreshold();

        settings.setEnabled(settingsComponent.isEnabled());
        settings.setFoldingThreshold(settingsComponent.getFoldingThreshold());

        if (wasEnabled != settings.isEnabled() || oldThreshold != settings.getFoldingThreshold()) {
            ApplicationManager.getApplication().invokeLater(this::refreshAllEditors);
        }
    }

    @Override
    public void reset() {
        PluginSettings settings = PluginSettings.getInstance();
        settingsComponent.setFoldingThreshold(settings.getFoldingThreshold());
        settingsComponent.setEnabled(settings.isEnabled());
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

    private void refreshAllEditors() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                FileDocumentManager.getInstance().saveAllDocuments();

                ApplicationManager.getApplication().runReadAction(() -> {
                    CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(project);

                    Editor[] editors = EditorFactory.getInstance().getAllEditors();
                    for (Editor editor : editors) {
                        if (!editor.isDisposed()) {
                            foldingManager.updateFoldRegions(editor);
                        }
                    }
                });

                DaemonCodeAnalyzer.getInstance(project).restart();
            }
        }
    }
}
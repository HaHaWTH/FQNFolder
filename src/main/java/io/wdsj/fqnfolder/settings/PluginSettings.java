package io.wdsj.fqnfolder.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "QualifiedNameFolderSettings",
        storages = @Storage("QualifiedNameFolderSettings.xml")
)
public class PluginSettings implements PersistentStateComponent<PluginSettings> {

    public int foldingThreshold = 16;
    public boolean enabled = true;

    public static PluginSettings getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettings.class);
    }

    @Nullable
    @Override
    public PluginSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PluginSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public int getFoldingThreshold() {
        return foldingThreshold;
    }

    public void setFoldingThreshold(int foldingThreshold) {
        this.foldingThreshold = foldingThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
package com.personal.brunohelper.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "BrunoHelperSettings", storages = @Storage("bruno-helper.xml"))
public final class BrunoHelperSettingsState implements PersistentStateComponent<BrunoHelperSettingsState.State> {

    private State state = new State();

    public static BrunoHelperSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(BrunoHelperSettingsState.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        this.state.bruCliPath = normalizeBruCliPath(this.state.bruCliPath);
        this.state.collectionOutputDirectory = this.state.collectionOutputDirectory == null
                ? ""
                : this.state.collectionOutputDirectory.trim();
    }

    public String getBruCliPath() {
        return normalizeBruCliPath(state.bruCliPath);
    }

    public void setBruCliPath(String bruCliPath) {
        state.bruCliPath = normalizeBruCliPath(bruCliPath);
    }

    public String getCollectionOutputDirectory() {
        return state.collectionOutputDirectory;
    }

    public void setCollectionOutputDirectory(String collectionOutputDirectory) {
        state.collectionOutputDirectory = collectionOutputDirectory == null ? "" : collectionOutputDirectory.trim();
    }

    public boolean isKeepTemporaryOpenApiFile() {
        return state.keepTemporaryOpenApiFile;
    }

    public void setKeepTemporaryOpenApiFile(boolean keepTemporaryOpenApiFile) {
        state.keepTemporaryOpenApiFile = keepTemporaryOpenApiFile;
    }

    public static final class State {
        public String bruCliPath = "";
        public String collectionOutputDirectory = "";
        public boolean keepTemporaryOpenApiFile = false;
    }

    private static String normalizeBruCliPath(String bruCliPath) {
        if (bruCliPath == null) {
            return "";
        }
        return bruCliPath.trim();
    }
}

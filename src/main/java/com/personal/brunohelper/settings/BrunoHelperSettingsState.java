package com.personal.brunohelper.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "EndpointCollectionExportSettings", storages = @Storage("endpoint-collection-export.xml"))
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
        this.state.collectionOutputDirectory = this.state.collectionOutputDirectory == null
                ? ""
                : this.state.collectionOutputDirectory.trim();
    }

    public String getCollectionOutputDirectory() {
        return state.collectionOutputDirectory;
    }

    public void setCollectionOutputDirectory(String collectionOutputDirectory) {
        state.collectionOutputDirectory = collectionOutputDirectory == null ? "" : collectionOutputDirectory.trim();
    }

    public List<String> getFieldBlacklistPatterns() {
        return state.fieldBlacklistPatterns == null ? new ArrayList<>() : new ArrayList<>(state.fieldBlacklistPatterns);
    }

    public void setFieldBlacklistPatterns(List<String> fieldBlacklistPatterns) {
        state.fieldBlacklistPatterns = fieldBlacklistPatterns == null ? new ArrayList<>() : new ArrayList<>(fieldBlacklistPatterns);
    }

    public static final class State {
        public String collectionOutputDirectory = "";
        public List<String> fieldBlacklistPatterns = new ArrayList<>();
    }
}

package de.uni_passau.fim.readability_plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.io.File;

/**
 * The plugin should behave differently depending on the open project.
 * This is necessary to test the plugin in a meaningful way as part of a study.
 *
 * Depending on whether certain files are in the project directory or not, certain features are activated or not.
 * This service class provides an interface for querying the plugin configuration in the current project.
 */
@Service(Service.Level.PROJECT)
public final class SettingsService {

    Project project;

    private boolean gitSyncEnabled;
    private boolean uiEnabled;

    public SettingsService(Project project) {
        this.project = project;

        File projectFile = new File(project.getBasePath());

        File gitSyncEnabledFile = new File(projectFile, "enable_git_sync");
        gitSyncEnabled = gitSyncEnabledFile.exists();

        File uiEnabledFile = new File(projectFile, "show_readability_ui");
        uiEnabled = uiEnabledFile.exists();

    }

    /**
     * Returns true if the "enable_git_sync" file existed in the project at the moment when the project was opened by
     * the
     * developer. Only if this file exists since the beginning the git sync service should be enabled.
     * @return whether the git sync is enabled for this project or not
     */
    public boolean useGitSync() {
        return gitSyncEnabled;
    }

    /**
     * Returns true if the "show_readability_ui file" existed in the project at the moment
     * when the project was opened by the developer.
     * Only if this file exists since the beginning the plugin ui features should be enabled.
     * @return whether the plugin ui is enabled for this project or not
     */
    public boolean showUi() {
        return uiEnabled;
    }


}

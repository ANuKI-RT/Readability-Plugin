package de.uni_passau.fim.readability_plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.uni_passau.fim.readability_plugin.services.SettingsService;
import org.jetbrains.annotations.NotNull;

/**
 * !!! FEATURE IS NOT YET IMPLEMENTED !!!
 *
 * This feature was planned during the proposal phase of the study but never implemented.
 * The idea was to rate the readability of a complete file by clicking on a corresponding button if the menu
 * appearing when a file was right-clicked.
 *
 * In future versions this class can serve as a starting point for the feature.
 */
public class TriggerReadabilityForSelectedFile extends AnAction {

    /**
     * Overriding the update method to detect if a selected file is a java file.
     * Only if this is the case the feature can be used.
     * Therefore, only if this is the case and the plugin ui is enabled the feature will be displayed in the menu
     * appearing if the file was right-clicked.
     * @param event
     */
    @Override
    public void update(@NotNull AnActionEvent event) {

        VirtualFile selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());

        if(selectedFile == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }
        boolean isJavaFile = selectedFile.getFileType().getDefaultExtension().equals("java");

        Project eventProject = event.getProject();
        boolean showUi;

        if(eventProject != null) {
            SettingsService settingsService = eventProject.getService(SettingsService.class);
            showUi = settingsService.showUi();
        }
        else {
            showUi = false;
        }

        boolean isVisible = isJavaFile && showUi;

        event.getPresentation().setEnabledAndVisible(isVisible);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("Hello World");
    }
}

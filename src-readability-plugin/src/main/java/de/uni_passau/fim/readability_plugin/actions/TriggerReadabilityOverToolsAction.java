package de.uni_passau.fim.readability_plugin.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityApi;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityProcessException;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityResult;
import de.uni_passau.fim.readability_plugin.readability.ScalabrinoReadabilityApi;
import de.uni_passau.fim.readability_plugin.services.SettingsService;
import org.jetbrains.annotations.NotNull;

/**
 * !!! FEATURE IS NOT YET IMPLEMENTED !!!
 *
 * This feature was planned during the proposal phase of the study but never implemented.
 * The idea was to rate the readability of a complete project using an option in the "tools" menu.
 *
 * In future versions this class can serve as a starting point for the feature.
 */
public class TriggerReadabilityOverToolsAction extends AnAction {

    private ReadabilityApi api;
    TriggerReadabilityOverToolsAction() {
        super();
        api = ScalabrinoReadabilityApi.getInstance();
    }
    @Override
    public void update(@NotNull AnActionEvent event) {

        Project project = event.getProject();

        boolean isVisible;

        if(project == null) {
            isVisible = false;
        }
        else {
            SettingsService settingsService = project.getService(SettingsService.class);
            isVisible = settingsService.showUi();
        }

        event.getPresentation().setEnabledAndVisible(isVisible);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {


        FileEditorManager manager = FileEditorManager.getInstance(event.getProject());

        VirtualFile files[] = manager.getSelectedFiles();

        for(VirtualFile file: files) {
            String filePath = file.getPath();
            try {
                //TODO hier wird ein string benötigt
                ReadabilityResult result = api.processCodeSnippet(filePath,false,null);
                System.out.println("Your file achieved the score  " +  result.getReadability());
                System.out.println( "Readability Result for " + file.getName());

            }
            catch (ReadabilityProcessException error) {
                System.err.println("Failed to process file " + filePath + " from toolbar:");
                System.err.println(error);
            }
        }

    }
}

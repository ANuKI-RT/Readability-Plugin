package de.uni_passau.fim.readability_plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityApi;
import de.uni_passau.fim.readability_plugin.readability.ScalabrinoReadabilityApi;
import de.uni_passau.fim.readability_plugin.dialogues.MarkedCodeDialogue;
import de.uni_passau.fim.readability_plugin.services.MetaDataService;
import de.uni_passau.fim.readability_plugin.services.SettingsService;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of AnAction class serving as starting point for the "Analyze readability for parent scope" feature.
 * This class is registered in the plugin.xml in order to display the option in the menu appearing when a marked code
 * is right-clicked.
 */
public class TriggerReadabilityForMarkedCodeAction extends AnAction {

    private ReadabilityApi api;
    TriggerReadabilityForMarkedCodeAction() {
        super();
        api = ScalabrinoReadabilityApi.getInstance();
    }

    /**
     * Overriding the update method in order to detect if code is selected when right clicking the
     * editor. Only if this is the case and the plugin ui is enabled the "Analyze readability for parent scope"
     * is visible in the menu.
     * @param event
     */
    @Override
    public void update(@NotNull AnActionEvent event) {

        DataContext context = event.getDataContext();
        Editor editor = CommonDataKeys.EDITOR.getData(context);
        SelectionModel selector = editor.getSelectionModel();

        Project eventProject = event.getProject();
        SettingsService settingsService = eventProject.getService(SettingsService.class);

        boolean isVisible = selector.hasSelection() && settingsService.showUi();

        event.getPresentation().setEnabledAndVisible(isVisible);
    }

    /**
     * Overriding the actionPerformed method in order to analyze the marked code if the
     * menu option "Analyze readability for parent scope" was selected. After the code was analyzed a plugin will
     * appear, displaying the evaluated readability score.
     * @param event
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        DataContext context = event.getDataContext();
        Editor editor = CommonDataKeys.EDITOR.getData(context);
        SelectionModel selector = editor.getSelectionModel();
        if(!selector.hasSelection()) {
            return;
        }

        String selectedSnippet = selector.getSelectedText();

        Project project = event.getProject();

        MetaDataService metaDataService = project.getService(MetaDataService.class);

        metaDataService.registerStartMarkedCodeRating(selectedSnippet);
        MarkedCodeDialogue detailDialogue = new MarkedCodeDialogue(selector);
        detailDialogue.show();
        if(detailDialogue.didFinishRating()) {
            metaDataService.registerFinishedMarkedCodeRating(selectedSnippet, detailDialogue.getResult());
        }
        else {
            metaDataService.registerCancelMarkedCodeRating(selectedSnippet);
        }

    }
}

package de.uni_passau.fim.readability_plugin.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityProcessException;
import de.uni_passau.fim.readability_plugin.services.ReadabilityService;
import org.jetbrains.annotations.NotNull;

/**
 * This class will be registered as an event listener if a new project is opened.
 * It will listen for files to be opened in a new editor within the current project.
 * If a new file was opened and the file is a .java file the listener triggers a readability rating to get initial
 * values for the java methods in the file.
 * Those readability values are required to add the coloured gutter icons.
 */
public class ProjectBoundedEditorListener implements FileEditorManagerListener {

    private Project project;
    private  ReadabilityService readabilityService;

    ProjectBoundedEditorListener(Project project) {
        super();
        this.project = project;
        this.readabilityService = project.getService(ReadabilityService.class);

    }

    /**
     * Checks if the opened file must be rated by the plugin.
     * If that is the case, the rating service will initially rate the file content and
     * show the coloured gutter icons to represent the readability.
     * @param source
     * @param file
     */
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        try {
            if(javaOfProjectDidChange(file)) {
                readabilityService.updateReadability(file);
            }
        }
        catch (ReadabilityProcessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Check if a given file is a .java file and belongs to the currently opened projects.
     * @param file the file to check the conditions for
     * @return whether both conditions are met or not
     */
    private boolean javaOfProjectDidChange(@NotNull VirtualFile file) {

        try {
            FileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            // Check if the file belongs to the project's content roots

            boolean isInContent = ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
                return fileIndex.isInContent(file);
            });
            if(!isInContent) {
                return false;
            }

            String fileType = file.getFileType().getDefaultExtension();
            return fileType.equalsIgnoreCase("java");
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

}


package de.uni_passau.fim.readability_plugin.listeners;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiJavaFile;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityProcessException;
import de.uni_passau.fim.readability_plugin.services.ReadabilityService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This class is registered as event listener if a new project is opened.
 * It will listen for file changes withing the files opened in the editor.
 * A file change event will be triggered after the file were saved by the developer.
 * If the listener recognizes files changes in a java file he triggers a re-rating of the methods readability
 * in order to update the coloured gutter icons.
 */
public class ProjectBoundedFileListener implements BulkFileListener {

    Project project;
    ReadabilityService readabilityService;

    ProjectBoundedFileListener(Project project) {
        super();
        this.project = project;
        this.readabilityService = project.getService(ReadabilityService.class);

    }

    /**
     * Checks if any of the update events corresponds to a java file within the currently opened project,
     * and if that is the case, re-rates its readability in order to update the gutter icons.
     * @param events
     */
    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.after(events);
        CompletableFuture.runAsync(() -> {
            for (VFileEvent event: events) {
                if(javaOfProjectDidChange(event)) {
                    processCodeUpdateEvent(event);
                }
            }
        });

    }

    /***
     * Re-rates the readability of all methods of a java file and prints an error if
     * the process failed.
     * @param event the event associated to the updated java file
     */
    private void processCodeUpdateEvent(VFileEvent event) {
        try{
            readabilityService.updateReadability(event.getFile());
        }
        catch (ReadabilityProcessException error) {
            System.err.println("Failed to process file: " + event.getFile().getPath() + " after file update:");
            System.out.println(error);
        }
    }

    /**
     * Check if a given file update event corresponds to a .java file belonging to the currently opened projects.
     * @param event the file update event to check the conditions for
     * @return whether both conditions are met or not
     */
    private boolean javaOfProjectDidChange(@NotNull VFileEvent event) {

        try {
            if(!event.isFromSave() && !event.isFromRefresh()) {
                return false;
            }

            VirtualFile file = event.getFile();
            FileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            // Check if the file belongs to the project's content roots

            //Application app = ApplicationManager.getApplication(); todo request read lock
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

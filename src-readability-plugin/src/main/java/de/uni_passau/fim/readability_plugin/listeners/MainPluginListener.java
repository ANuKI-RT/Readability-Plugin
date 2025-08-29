package de.uni_passau.fim.readability_plugin.listeners;

import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import de.uni_passau.fim.readability_plugin.services.GitSyncService;
import de.uni_passau.fim.readability_plugin.services.MetaDataService;

import java.util.HashMap;
import java.util.Map;

/**
 * This class servers as the main entry point of the plugin.
 *
 * The plugin works project scopes.
 * This means that everytime a project is opened in the IDE the following plugin functionality is started.
 *
 * !!! DEPRECATION WARNING !!!
 *
 * Unfortunately the projectOpened function was marked as deprecated and will therefore be removed in future versions.
 * New Plugin version will therefore a refactoring of the plugins main entry point
 */
public class MainPluginListener implements ProjectManagerListener {

    private Map<Project,MessageBusConnection> projectListenerConnections;

    MainPluginListener() {
        projectListenerConnections = new HashMap<Project,MessageBusConnection>();
    }

    /**
     * Starting the plugins services if a new project is opened.
     * The following services are started:
     *
     * 1. Starting the git sync service for the study if it is enabled
     * 2. Starting the monitoring service for the study if it is enabled
     * 3. Displaying the plugins tool window on the left side of the editor
     *
     * 4. adding a listener to evaluate readability if a new editor window with java code is opened
     * 5. adding a listener to re-evaluate java code if it was changed
     *
     * @param project the opened IDE project
     */
    @Override
    public void projectOpened(Project project) {
        System.out.println("Project opened: " + project.getName());
        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new ProjectBoundedFileListener(project));
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new ProjectBoundedEditorListener(project));
        connection.subscribe(ToolWindowManagerListener.TOPIC, new PluginWindowListener(project));

        GitSyncService gitSyncService = project.getService(GitSyncService.class);
        gitSyncService.startSyncing(1);


        MetaDataService metaDataService = project.getService(MetaDataService.class);
        metaDataService.init();


        projectListenerConnections.put(project,connection);
    }

    /**
     * Removing all listeners of a project is it was closed.
     * @param project the closed project
     */
    @Override
    public void projectClosed(Project project) {
        MessageBusConnection connection = projectListenerConnections.get(project);
        if(connection != null) {
            connection.disconnect();
        }
    }
}


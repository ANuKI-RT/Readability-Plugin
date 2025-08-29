package de.uni_passau.fim.readability_plugin.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import de.uni_passau.fim.readability_plugin.services.SettingsService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * If the plugin ui is disabled the tool window must be removed.
 * Therefore, this Listener does react on new registered tool windows and disables the
 * plugin window if it was registered within a project that has the plugin ui disabled.
 */
public class PluginWindowListener implements ToolWindowManagerListener {

    Project project;
    SettingsService settingsService;

    PluginWindowListener(Project project) {
        super();
        this.project = project;
        this.settingsService = project.getService(SettingsService.class);
    }

    /**
     * Checking if the id of the registered tool window match the ReadabilityPlugin window.
     * If that is the case and the plugin ui id disabled the tool window will be removed.
     * @param ids the id's of registered tool windows
     * @param toolWindowManager management api for tool windows
     */
    @Override
    public void toolWindowsRegistered(@NotNull List<String> ids, @NotNull ToolWindowManager toolWindowManager) {
        try {
            if(!ids.contains("ReadabilityPlugin")) {
                return;
            }
            ToolWindow pluginWindow = toolWindowManager.getToolWindow("ReadabilityPlugin");
            if(pluginWindow != null && !settingsService.showUi()) {
                pluginWindow.remove();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}


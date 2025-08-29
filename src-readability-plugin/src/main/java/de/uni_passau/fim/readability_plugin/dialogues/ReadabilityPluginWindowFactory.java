package de.uni_passau.fim.readability_plugin.dialogues;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * This class serves as a util class to the configure the tool window of the plugin.
 * It must be registered in the plugin.xml
 */
public class ReadabilityPluginWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.setTitle("Readability Plugin");
    }
}

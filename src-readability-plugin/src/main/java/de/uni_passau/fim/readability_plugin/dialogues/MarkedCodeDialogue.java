package de.uni_passau.fim.readability_plugin.dialogues;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import de.uni_passau.fim.readability_plugin.java_parsing.JavaFileParser;
import de.uni_passau.fim.readability_plugin.java_parsing.JavaParseTree;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityResult;
import de.uni_passau.fim.readability_plugin.services.MetaDataService;
import de.uni_passau.fim.readability_plugin.services.ReadabilityService;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * This dialogue should appear when a user triggers the "Analyze readability for parent scope" feature.
 * The PopUp Dialogue includes the evaluated readability score and the start and end lines of the evaluated code.
 */
public class MarkedCodeDialogue extends DialogWrapper {

    private final String selectedSnippet;
    private final ReadabilityService readabilityService;
    private final MetaDataService metaDataService;
    private boolean ratingDidFinish = false;
    private ReadabilityResult result;
    private Editor editor;
    private Project project;
    private VirtualFile selectedFile;
    private int codeStartOffset;
    private int codeEndOffset;


    public MarkedCodeDialogue(SelectionModel selection) {
        super(true); // use current window as parent

        editor = selection.getEditor();
        project = editor.getProject();
        selectedFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        readabilityService = project.getService(ReadabilityService.class);
        metaDataService = project.getService(MetaDataService.class);
        selectedSnippet = selection.getSelectedText();
        codeStartOffset = selection.getSelectionStart();
        codeEndOffset = selection.getSelectionEnd();


        setTitle("Readability Rating");
        init();
    }

    /**
     * This method will rate the given code snippet and then display the result and scope borders in the
     * dialogue.
     *
     * While the code is not completely rated a loading screen will be rendered.
     * @return the panel including the dialogue content
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("Please wait while the plugin rates the marked snippet ...");
        label.setPreferredSize(new Dimension(100, 50));
        dialogPanel.add(label, BorderLayout.CENTER);

        JavaFileParser fileParser = new JavaFileParser(project,selectedFile);
        JavaParseTree surroundingScope = fileParser.getSurroundingScope(codeStartOffset,codeEndOffset);

        SwingWorker<ReadabilityResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ReadabilityResult doInBackground() throws Exception {

                return readabilityService.rateSnippet(surroundingScope.getCode(),surroundingScope.getScopeElement());
            }

            @Override
            protected void done() {
                try {
                    result = get();
                    ratingDidFinish = true;
                    StringBuilder scopeDescription = new StringBuilder()
                            .append("Readability of scope ").append(surroundingScope.getName())
                            .append(" starting at line: ").append(surroundingScope.getStartLine())
                            .append(" and ending at line ").append(surroundingScope.getEndLine())
                            .append(":");
                    label.setText(scopeDescription.toString());
                    String roundedReadability = String.format("%.2f", result.getReadability());
                    dialogPanel.add(new JLabel(roundedReadability),BorderLayout.AFTER_LAST_LINE);
                } catch (Exception ex) {
                    // Handle any exceptions that may occur during processing
                    ex.printStackTrace();
                        label.setText("Error occurred while processing snippet.");
                }
            }


        };
        worker.execute();

        return dialogPanel;
    }

    /**
     * A user can cancel the rating of the code.
     * This method does only return true if the evaluation was not cancelled and
     * therefore successfully completed
     * @return whether the rating was finished or not
     */
    public boolean didFinishRating() {
        return ratingDidFinish;
    }

    /**
     * Provide the readability result associated with the values displayed
     * in the dialogue
     * @return the associated readability result
     */
    public ReadabilityResult getResult() {
        return result;
    }

    /**
     * Create a cancel action in the createActions method, in order to enable
     * the user to cancel the readability evaluation.
     * @return a list of the created actions including the cancel action
     */
    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction()};
    }
}
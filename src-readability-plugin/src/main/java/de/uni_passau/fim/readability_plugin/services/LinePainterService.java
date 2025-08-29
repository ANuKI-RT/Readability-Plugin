package de.uni_passau.fim.readability_plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.uni_passau.fim.readability_plugin.painting.RatedJavaScope;
import de.uni_passau.fim.readability_plugin.painting.ReadabilityColorRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This service class can invoke a re-coloring of the java methods of a java class.
 * The coloring process will add gutter icons next to the code lines in the editor.
 * The color of the icons is defined by the readability of a method.
 */
@Service(Service.Level.PROJECT)
public final class LinePainterService {

    private final Project project;
    private final MetaDataService metaDataService;
    LinePainterService(Project project) {
        this.project = project;
        this.metaDataService = project.getService(MetaDataService.class);
    }

    /**
     * Applies coloured icons to all methods of a java file.
     * The colours are defined by the readability score of the methods.
     * The gutter icons are set in all editor windows that have the given file opened.
     * @param file the file to re-color the methods for
     * @param ratedMethods the ratings of all the java classes methods
     */
    public synchronized void colorLines(VirtualFile file, List<RatedJavaScope> ratedMethods) {

        FileEditor[] fileEditors = FileEditorManager.getInstance(project).getAllEditors(file);
        for (FileEditor fileEditor : fileEditors) {

            if (!(fileEditor instanceof TextEditor)) {
                continue;
            }

            Editor editor = ((TextEditor) fileEditor).getEditor();

            MarkupModel markupModel = editor.getMarkupModel();
            RangeHighlighter[] highlighters = markupModel.getAllHighlighters();

            for(RangeHighlighter highlighter: highlighters) {
                if(highlighter.getGutterIconRenderer() instanceof ReadabilityColorRenderer) {
                    markupModel.removeHighlighter(highlighter);
                }
            }

            for(RatedJavaScope ratedMethod : ratedMethods) {

                int startLine = ratedMethod.getStartIndex();
                int endLine = ratedMethod.getEndIndex();

                GutterIconRenderer renderer = new ReadabilityColorRenderer(ratedMethod,editor);

                for (int i = startLine; i <= endLine ; i++) {
                    RangeHighlighter highlighter = markupModel.addLineHighlighter(i, 0,null);
                    //highlighter.setErrorStripeMarkColor();
                    highlighter.setGutterIconRenderer(renderer);
                    
                    // Set to false if you don't want the range highlighter to be drawn over text selection
                    highlighter.setGreedyToLeft(true);
                    highlighter.setGreedyToRight(true);
                }

            }


        }
    }





}
package de.uni_passau.fim.readability_plugin.painting;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import de.uni_passau.fim.readability_plugin.dialogues.ReadabilityDetailsDialogue;
import de.uni_passau.fim.readability_plugin.services.MetaDataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * The class can be used to render a gutter icon next to a code line in the editor.
 * Depending on the readability associated to the method the code line belongs to,
 * the icon will display a line in either green (well readable), yellow (medium readable) or red (poorly readable)
 * colour.
 */
public class ReadabilityColorRenderer extends GutterIconRenderer {
    private final Color color;
    private final RatedJavaScope ratedJavaMethod;
    private final Editor editor;
    public ReadabilityColorRenderer(RatedJavaScope ratedJavaMethod, Editor editor) {

        this.color = ratedJavaMethod.getAssociatedColor();
        this.ratedJavaMethod = ratedJavaMethod;
        this.editor = editor;
    }

    /**
     * The gutter icon can be right click to show a menu that can be used to trigger all
     * features related to the "readability details" part of the plugin.
     * This menu provides the action objects related to the menu button that can be used to start the features.
     * The following actions (and therefor menu options) are provided:
     *
     * 1. Show Readability Tree
     * 2. Give hints for improving readability
     *
     * @return the menu actions
     */
    @Override
    public @Nullable ActionGroup getPopupMenuActions() {
        return new ActionGroup() {
            @Override
            public @Nullable AnAction[] getChildren(@Nullable AnActionEvent e) {
                // Create and return the "Show Details" action using an anonymous inner class
                return new AnAction[]{
                        new AnAction("Show Readability Tree") {
                            @Override
                            public void actionPerformed(@Nullable AnActionEvent e) {
                                ReadabilityDetailsDialogue details = new ReadabilityDetailsDialogue(editor,ratedJavaMethod);

                                details.showTreeWithRatings();
                                MetaDataService metaDataService = editor.getProject().getService(MetaDataService.class);
                                metaDataService.registerDisplayDetails(ratedJavaMethod,"show-readability-scope-tree");
                            }
                        },

                        new AnAction("Give hints for improving readability") {
                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e) {
                                ReadabilityDetailsDialogue details = new ReadabilityDetailsDialogue(editor, ratedJavaMethod);
                                details.showImprovementHints();
                                MetaDataService metaDataService = editor.getProject().getService(MetaDataService.class);
                                metaDataService.registerDisplayDetails(ratedJavaMethod,"show-improvement-hints");
                            }
                        }

                };
            }
        };
    }

    @Override
    public @Nullable String getTooltipText() {
        SwingUtilities.invokeLater(() -> {
            MetaDataService metaDataService = editor.getProject().getService(MetaDataService.class);
            metaDataService.registerGutterIconHover(ratedJavaMethod);
        });
        String roundedRating = String.format("%.2f", ratedJavaMethod.getRating());
        return "Readability Score: " + roundedRating;
    }

    /**
     * Provides the icon to be rendered.
     * In our case this is a line in either green, yellow or red colour.
     * @return
     */
    @Override
    public Icon getIcon() {
        return new LineIcon(color);
    }

    /**
     * Two line icons equal each other if and only if both the colours associated with
     * the line icons match.
     * Of course the given object can only be equal to the current one if it is also a instance of
     * ReadabilityColorRenderer.
     *
     * @param obj the object to compare with the current ReadabilityColorRenderer instance
     * @return whether the two object are equal or not
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReadabilityColorRenderer && color.equals(((ReadabilityColorRenderer) obj).color);
    }

    /**
     * The hash code of the icon is represented by the hash code of the colour
     * that is displayed in the icon.
     * @return the lines icons hash code
     */
    @Override
    public int hashCode() {
        return color.hashCode();
    }

    /**
     * Provides the Gutter Icon alignment.
     * In our case this icons will always be left aligned
     * @return the alignment object to define the alignment
     */
    @Override
    public Alignment getAlignment() {
        return Alignment.LEFT;
    }

    /**
     * This class creates line-icons a new icon type.
     * These line icons are the icons rendered by the ReadabilityColorRenderer.
     *
     * Within the overwritten methods the color width and height of the icons are provided
     */
    private static class LineIcon implements Icon {
        private static final int LINE_WIDTH = 10;
        private final Color color;

        /**
         * Takes the colour the line icon should have as constructor parameter
         * @param color the line icon colour
         */
        public LineIcon(Color color) {

            this.color = color;
        }

        /**
         * Paints a line icon with the given color
         * @param c  a {@code Component} to get properties useful for painting
         * @param g  the graphics context
         * @param x  the X coordinate of the icon's top-left corner
         * @param y  the Y coordinate of the icon's top-left corner
         */
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.fillRect(x + (getIconWidth() - LINE_WIDTH) / 2, y, LINE_WIDTH, getIconHeight());
        }


        /**
         * Set the line width to the static LINE_WIDTH attribute
         * @return the line width
         */
        @Override
        public int getIconWidth() {
            return LINE_WIDTH;
        }

        /**
         * Set the line height to 20.
         *
         * TODO: this should be stored in a class attribute like LINE_WIDTH
         * @return the line height
         */
        @Override
        public int getIconHeight() {
            return 20; // Adjust the height of the icon as needed
        }
    }
}

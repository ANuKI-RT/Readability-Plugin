package de.uni_passau.fim.readability_plugin.painting;


import de.uni_passau.fim.readability_plugin.java_parsing.JavaParseTree;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityResult;

import java.awt.*;
import java.util.List;

/**
 * This class represents a rated java method.
 * If consists of a JavaParseTree instance for the method and a readability ratings for
 * the methods code.
 *
 * It offers utility methods to get information about the rated method.
 */
public class RatedJavaScope {
    private  ReadabilityResult result;
    private JavaParseTree javaScope;

    public RatedJavaScope(JavaParseTree javaScope, ReadabilityResult result) {
        this.javaScope = javaScope;
        this.result = result;
    }

    /**
     * Get the zero-indexed start line index of the rated method.
     * @return the start index
     */
    public int getStartIndex() {
        return javaScope.getStartLine(true);
    }


    /**
     * Get the zero-indexed end line index of the rated method
     * @return the end index
     */
    public int getEndIndex() {
       return javaScope.getEndLine(true);
    }

    /**
     * Get the colour that corresponds to the readability rating of the java code.
     * Bad code will result in red, medium readable code in yellow and well readable code in green.
     * @return the associated color
     */
    public Color getAssociatedColor() {
        double readabilityScore = result.getReadability();
        if(readabilityScore < 0.33) {
            return Color.RED;
        }
        if(readabilityScore > 0.66) {
            return Color.GREEN;
        }

        return Color.YELLOW;
    }

    /**
     * Returns true if the rated method has a java doc comment.
     * @return wether the method has a java doc comment or not
     */
    public boolean hasJavaDoc() {
        List<JavaParseTree> scopeCommentList = javaScope.search(JavaParseTree.docComment);
        return !scopeCommentList.isEmpty();
    }

    /**
     * Provide the readability rating score of the method.
     * @return the readability rating
     */
    public double getRating() {
        return result.getReadability();
    }

    /**
     * Provide the complete readability result for the method including the values
     * of all the different readability metrics.
     * @return the readability result
     */
    public ReadabilityResult getReadabilityResult() {
        return result;
    }

    /**
     * The java method has a parse tree assigned in order to interact with the abstract parse tree
     * of the code. This parse tree is provided by this getter.
     * @return the associated parse tree instance representing the method scope
     */
    public JavaParseTree getAssociatedScope() {
        return javaScope;
    }

    /**
     * Get the name of the rated method
     * @return the method name
     */
    public String getMethodName() {
        return javaScope.getName().replace("method ","");
    }

    /**
     * Get the directory path mapping to the rated file.
     * @return the files directory path
     */
    public String getJavaFilePath() {
        return javaScope.getJavaFile().getPath();
    }

    /**
     * Provides the source code of the rated java method.
     * @return the methods source code
     */
    public String getCode() {
        return javaScope.getCode();
    }


}

package de.uni_passau.fim.readability_plugin.readability;

import com.intellij.psi.PsiElement;

/**
 * Defines a method that every readability api should implement.
 * Different readability api's are required to support different readability models
 */
public interface ReadabilityApi {

    /**
     * Process a source code snippet in order to get its readability.
     * If the api implements a metric based the metrics should also be extractable,
     * if the "attachMetrics" flag is provided.
     *
     * Some models may require information about the scope of the rated java code within the parse tree
     * of the complete java class.
     * This scope information must be passed by passing an instance of PsiElement that is associated with
     * the given code snippet.
     *
     * @param codeSnippet the code snippet to be evaluated
     * @param attachMetrics whether to attach code metrics or not
     * @param linkedPsiElement the java parse tree element linked to the provided source code
     * @return the Readability Rating Result
     * @throws ReadabilityProcessException if an error appeared during the rating process
     */
    public ReadabilityResult processCodeSnippet(String codeSnippet, boolean attachMetrics,
                                                PsiElement linkedPsiElement) throws ReadabilityProcessException;

}

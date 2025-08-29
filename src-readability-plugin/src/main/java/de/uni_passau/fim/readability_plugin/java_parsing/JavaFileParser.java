package de.uni_passau.fim.readability_plugin.java_parsing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;

import java.util.List;

/**
 * This class is used to represent all the methods of a java class,
 * It provides util method to get information about the classes source code and the method in it.
 */
public class JavaFileParser {
    private PsiJavaFile psiJavaFile;

    private JavaParseTree root;

    private VirtualFile virtualJavaFile;

    /**
     * In order to parse a given java file the associated virtual file
     * and the currently opened IDE Project is required.
     * @param project
     * @param javaFile
     */
    public JavaFileParser(Project project, VirtualFile javaFile) {
        virtualJavaFile = javaFile;

        PsiManager psiManager = PsiManager.getInstance(project);
        Application app = ApplicationManager.getApplication();
        psiJavaFile = app.runReadAction((Computable<PsiJavaFile>) () -> (PsiJavaFile) psiManager.findFile(javaFile));
        root = new JavaParseTree(psiJavaFile,psiJavaFile,null);
    }

    /**
     * Get the parse trees of all the methods of the class
     * @return the parse trees
     */
    public List<JavaParseTree> getMethods() {
        return root.search(PsiMethod.class);
    }

    /**
     * get the source code of the java class
     * @return the code as a String
     */
    String getCode() {
        return root.getCode();
    }

    /**
     * Get the directory path linking to the file
     * @return the path as a String
     */
    String getPath() {
        return virtualJavaFile.getPath();
    }

    /**
     * Takes a start and end offset index and procides the parse tree of
     * the smallest java scopes surrounding the code beginning at the start offset and ending at the
     * end offset.
     *
     * offset here defines the index of a character in the java source file if the java files content is represented
     * as a character array
     * @param startOffset the start offset
     * @param endOffset the end offset
     * @return the parse tree of the surrounding java scope
     */
    public JavaParseTree getSurroundingScope(int startOffset, int endOffset) {

        int startLine = JavaParseTree.calcLineNumber(getCode(),startOffset);
        int endLine = JavaParseTree.calcLineNumber(getCode(),endOffset);

        return root.getSurroundingScope(startLine,endLine);
    }

}

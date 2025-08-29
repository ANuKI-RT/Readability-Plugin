package de.uni_passau.fim.readability_plugin.java_parsing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Java Parse Tree represents a specific java language element.
 * This element can be for example a variable declaration, assignment, comparisons etc...
 * Especially those elements can also be scopes. A scope is here defined as a part of the code beginning and
 * ending with curly brackets.
 *
 * This class is a wrapper class arround a specific java element and can be used to extract information about it.
 * This can be:
 * - start line number
 * - end line number
 * - scope type (for example for block etc...) if elemen is a scope
 * - the source code itself
 * - information about surrounding code scopes
 * etc...
 */
public class JavaParseTree {

    public static List<Class> scopes = List.of(
            PsiMethod.class,
            PsiClass.class,
            PsiForStatement.class,
            PsiWhileStatement.class,
            PsiDoWhileStatement.class,
            PsiIfStatement.class,
            PsiSwitchStatement.class,
            PsiTryStatement.class,
            PsiCatchSection.class,
            PsiJavaFile.class,
            PsiForeachStatement.class
    );

    public static Class docComment = PsiDocComment.class;
    public static Class methodRoot = PsiMethod.class;
    public static List<Class> codeBlocks = List.of(PsiCodeBlock.class, PsiBlockStatement.class);


    private List<JavaParseTree> children;
    private JavaParseTree parent;
    private int startLineNumber;
    private int endLineNumber;
    private String scopeCode;
    private String scopeName;
    private PsiJavaFile javaFile;
    private PsiElement scopeElement;

    /**
     * Construct the scope using its associated objects in the psi library and the parent JavaParseTree scope object for
     * the constructed scope (the scopes are represented as a tree)
     * @param javaFile the psi java file
     * @param scopeElement the psi java scope
     * @param parent the parent parse tree instance
     */
    protected JavaParseTree(PsiJavaFile javaFile, PsiElement scopeElement, JavaParseTree parent) {
        this.children = new ArrayList<>();
        this.parent = parent;
        this.javaFile = javaFile;
        this.scopeElement = scopeElement;

        Application app = ApplicationManager.getApplication();

        scopeCode = app.runReadAction((Computable<String>) () -> scopeElement.getText());


        int startOffset = app.runReadAction((Computable<Integer>) () -> scopeElement.getTextRange().getStartOffset());
        int endOffset = app.runReadAction((Computable<Integer>) () -> scopeElement.getTextRange().getEndOffset());

        String javaFileCode = app.runReadAction((Computable<String>) () -> javaFile.getText());
        startLineNumber = calcLineNumber(javaFileCode,startOffset);
        endLineNumber = calcLineNumber(javaFileCode,endOffset);

        List<PsiElement> psiChildren =
                app.runReadAction((Computable<List<PsiElement>>) () -> Arrays.asList(scopeElement.getChildren()));

        scopeName = generateScopeName();

        for(PsiElement psiChild : psiChildren) {
            children.add(new JavaParseTree(javaFile,psiChild,this));
        }



    }

    /**
     * get the start line of the scope
     * @param zeroIndexed whether to provide the line number zero or one indexed
     * @return the start line number
     */
    public int getStartLine(boolean zeroIndexed) {
        return zeroIndexed ? startLineNumber : startLineNumber + 1;
    }

    /**
     * get the start line of the scope in one indexed format
     * @return the start line number
     */
    public int getStartLine() {
        return getStartLine(false);
    }

    /**
     * get the end line of the scope in one indexed format
     * @return the start line number
     */
    public int getEndLine() {
        return getEndLine(false);
    }

    /**
     * get the end line of the scope
     * @param zeroIndexed whether to provide the line number zero or one indexed
     * @return the end line number
     */
    public int getEndLine(boolean zeroIndexed) {
        return zeroIndexed ? endLineNumber : endLineNumber + 1;
    }

    /**
     * Get the actual source scope included in the java scope
     * @return the source code
     */
    public String getCode() {
        return scopeCode;
    }

    /**
     * Get the parent scope of the current parse tree
     * @return the parent parse tree
     */
    public JavaParseTree getParent() {
        return parent;
    }

    /**
     * Get all the child scopes included in the current parse tree scope
     * @return the child scopes as parse tree instances
     */
    public List<JavaParseTree> getChildren() {
        return children;
    }

    /**
     * Get the virtual file associated to the java file the java scope is in.
     * @return the virtual file istance
     */
    public VirtualFile getJavaFile() {
        return javaFile.getVirtualFile();
    }

    /**
     * Get the psi element linked to the parse tree scope
     * @return the associated psi element
     */
    public PsiElement getScopeElement() {
        return scopeElement;
    }

    /**
     * Returns true if the parse tree as no child scopes included and is
     * therefore a leaf in the scope tree.
     * @return whether the parse tree is a leaf or not
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Returns true if the parse tree represent the scope including
     * the complete java file and is therefore the root of the scope tree.
     * @return whether the parse tree instance is the root or not
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Get the scope name of the scope.
     * The name consists of the language construct implemented by the scope.
     * That can be for example:
     *
     * - a while loop
     * - a for loop
     * - an if statememt
     * etc....
     *
     * If the scope represents a method or complete class
     * the class/method name is included in the scopes name
     * @return the generated scope name
     */
    private String generateScopeName() {

        if(isMethodBody()) {
            return "method body";
        }

        String psiClass = scopeElement.getClass().getName();
        String[] classPath = psiClass.split("\\.");
        String psiClassName = classPath[classPath.length -1];

        String scopeName = psiClassName.toLowerCase()
                .replace("psi","")
                .replace("impl","")
                .replace("statement", " statement")
                .replace("comment", " comment");


        Application app = ApplicationManager.getApplication();
        if(scopeElement instanceof PsiMethod) {

            String methodName = app.runReadAction((Computable<String>) () -> ((PsiMethod) scopeElement).getName());
            scopeName = scopeName + " " + methodName;
        }
        else if(scopeElement instanceof PsiClass) {
            String className = app.runReadAction((Computable<String>) () -> ((PsiClass) scopeElement).getName());
            scopeName = scopeName + " " + className;
        }

        return scopeName;
    }

    /**
     * get the name of the scope created by the language construct and eventually class/method name
     * of the parse tree's scope
     * @return the scopes name
     */
    public String getName() {
        return scopeName;
    }

    /**
     * Search for children in the parse tree
     * @return all found children as a list
     */
    public List<JavaParseTree> search() {
        return search(PsiElement.class);
    }

    /**
     * Search for all children of a specific type.
     * The type is defined by the corresponding child psi element child class
     * the psi element linked to the parse tree has.
     *
     * @param targetScopeType the class to match the type of the scopes children.
     * @return all children matching the type of the given class.
     */
    public List<JavaParseTree> search(Class targetScopeType) {
        return search(List.of(targetScopeType));
    }

    /**
     * Search for all children matching specific types.
     * The types are defined by the corresponding child psi element child class
     * the psi element linked to the parse tree has.
     *
     * @param targetScopeTypes the classes to match the type of the scopes children.
     * @return all children matching any of the given scope type classes.
     */
    public List<JavaParseTree> search(@NotNull List<Class> targetScopeTypes) {
        List<JavaParseTree> result = new ArrayList<>();
        for(JavaParseTree child: children) {
            PsiElement childElem = child.scopeElement;
            boolean matchesAnyClass = false;
            for(Class targetClass : targetScopeTypes) {
                if(targetClass.isInstance(childElem)) {
                    matchesAnyClass = true;
                }
            }
            if(matchesAnyClass) {
                result.add(child);
            }
            result.addAll(child.search(targetScopeTypes));
        }
        return result;
    }

    /**
     * get the smallest surrounding scope for a given start and end line
     * of the java code the parse tree is in.
     * The scope is found by searching for children of the scope that have all the code from start to end line in it.
     * The child with the least lines if code (and therefore the smallest surrounding scope) is returned.
     * @param startLine the start line the scope should include
     * @param endLine the end line the sope should include
     * @return the found surrounding scope or null if no scope was found
     */
    public JavaParseTree getSurroundingScope(int startLine, int endLine) {
        if(isScope() && startLine >= this.startLineNumber && endLine <= this.endLineNumber) {
            JavaParseTree surrounding = this;
            for(JavaParseTree child: children) {
                JavaParseTree childSurrounding = child.getSurroundingScope(startLine,endLine);
                if(childSurrounding != null ) {
                    surrounding = childSurrounding;
                }
            }
            return surrounding;
        }
        return null;
    }

    /**
     * Get the line number the character matched by a given offset is in.
     * if the java code is represented as a character array the offset defined the index to identify a specific
     * character in the code.
     * @param code the source code the character defined by the offset is in
     * @param offset the offset defining the characters index in the scource code strig
     * @return the calculated line number (zero indexed)
     */
    protected static int calcLineNumber(String code, int offset) {
        String[] lines = code.split("\n");
        List<Integer> lineRanges = new ArrayList<Integer>();

        for(String line: lines ) {
            lineRanges.add(line.length()+1);
        }

        int prevRange = 0;
        for (int i = 0; i < lineRanges.size(); i++) {
            int range = prevRange + lineRanges.get(i);
            if(prevRange <= offset && range >= offset) {
                return i;
            }
            prevRange = range;
        }
        return lineRanges.size();
    }

    /**
     * This method returns true of the current java parse tree is a scope
     * @return whether the parse tree is a scope or not
     */
    public boolean isScope() {

        for(Class psiClass: scopes) {
            if(psiClass.isInstance(this.scopeElement)) {
                return true;
            }
        }
        return isMethodBody();
    }

    /**
     * This method returns true of the current java parse tree is code block.
     * @return whether the parse tree is a code block or not
     */
    public boolean isCodeBlock() {
        for(Class psiClass: codeBlocks) {
            if(psiClass.isInstance(this.scopeElement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method returns true of the current java parse tree is method body.
     * @return whether the parse tree is a method body or not
     */
    public boolean isMethodBody() {
        if(isRoot()) {
            return false;
        }
        return isCodeBlock() && methodRoot.isInstance(parent.scopeElement);
    }

    /**
     * This method returns true of the current java parse tree is doc comment.
     * @return whether the parse tree is a doc comment or not
     */
    public boolean isDocComment() {
        return docComment.isInstance(scopeElement);
    }
}

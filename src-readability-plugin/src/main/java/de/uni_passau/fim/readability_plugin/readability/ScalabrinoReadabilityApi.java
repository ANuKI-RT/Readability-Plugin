package de.uni_passau.fim.readability_plugin.readability;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Implementation of the ReadabilityApi interface to support
 * the Metric Based Readability Model Released by Scalabrino in 2018
 */
public class ScalabrinoReadabilityApi implements ReadabilityApi {

    private final File tempDir;
    private final String rseJarPath;

    private static ScalabrinoReadabilityApi instance;


    /**
     * ScalabrinoReadabilityApi is implemented as singleton as only one instance is required.
     * @return the singleton instance of the ScalabrinoReadabilityApi
     */
    public static ScalabrinoReadabilityApi getInstance() {
        if(instance == null) {
            instance = new ScalabrinoReadabilityApi();
        }
        return instance;
    }

    /**
     * Constructs a new ScalabrinoReadabilityApi
     * @throws RuntimeException if no model binary was found or no dir to place the code snippets and could be created.
     */
    private ScalabrinoReadabilityApi() throws RuntimeException {
        try {
            tempDir = findTempDir();
            rseJarPath = findRseJarPath();

        }
        catch (NullPointerException | IOException error) {
            System.err.println("Failed to initiate ReadabilityApi when trying create path strings to model or temp dir");
            System.err.println(error);
            throw new RuntimeException(error);
        }
    }

    /**
     * Identified to directory path pointing to the Scalabrino Model RSE.jar used by this api.
     * By default, the model should be placed in the plugin path the Readability Model is installed in.
     * If the plugin is executed in debug mode while developing the model can also be placed within the project
     * directory of the plugin. The model file ist first searched in the plugin install dir and if it was not found
     * the path within the plugin project in IntelliJ is returned.
     *
     * @return the path to the RSE.jar file
     */
    private String findRseJarPath() {
        String pluginPath = PathManager.getPluginsPath() + File.separator + "CodeReadabilityPlugin";
        String modelPath = pluginPath + File.separator + "bin" + File.separator + "readability_model";
        if(new File(modelPath).exists()) {
            return modelPath;
        }
        File devRoot = new File(pluginPath)
                .getParentFile() //plugins
                .getParentFile() //idea-sandbox
                .getParentFile() //build
                .getParentFile(); //dev root
        String devPath = devRoot.getPath() + File.separator + "src" + File.separator + "main" + File.separator +
                "resources" + File.separator + "readability_model";
        return devPath;

    }

    /**
     * For the readability model a temprary directory is required in order to place the code snippets that should be
     * rated in it.
     *
     * By default, this temp dir should exist within the path the plugin is installed in.
     * If this is not the case a new temporary directory is created using the Files.createTempDirectory API.
     * This method searches for the temp dir and returns the directory path for it.
     *
     * @return the path pointing to the temp dir
     * @throws IOException if no temp exists and no new temp dir could be created
     */
    private File findTempDir() throws IOException {
        String pluginPath = PathManager.getPluginsPath() + File.separator + "CodeReadabilityPlugin";
        File temp = new File(pluginPath + File.separator + "tmp");
        if(temp.exists()) {
            return temp;
        }
        return Files.createTempDirectory("uni_passau_se2_readability_plugin_").toFile();

    }

    /**
     * TODO: Remove - this main method was present for debugging purposes
     * @param args
     */
    public static void main(String[] args) {

    }

    /**
     * Rates the readability by calling the RSE.jar in a terminal process and parsing the
     * stdout of the process.
     *
     * @param codeSnippet the code snippet to be evaluated
     * @param attachMetrics whether to attach code metrics or not
     * @param linkedPsiElement the java parse tree element linked to the provided source code
     * @return
     * @throws ReadabilityProcessException if any issued appear during the code rating process
     */
    @Override
    public ReadabilityResult processCodeSnippet(String codeSnippet, boolean attachMetrics, PsiElement linkedPsiElement) throws ReadabilityProcessException {

        File tempSnippetFile;

        try {

            String wrappedClassSnippet = codeSnippet;

            if(!(linkedPsiElement instanceof PsiJavaFile)) {
                if(!(linkedPsiElement instanceof PsiMethod)) {
                    wrappedClassSnippet = "    public static void main(String[] args) {\n"+wrappedClassSnippet+"\n    }";
                }
                wrappedClassSnippet = "public class Main() {\n"+wrappedClassSnippet+"\n}";
            }

            tempSnippetFile = File.createTempFile("snippet",".java", tempDir);
            tempSnippetFile.setWritable(true);
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempSnippetFile));
            writer.write(wrappedClassSnippet);
            writer.close();
        }
        catch (IOException error) {
            throw new ReadabilityProcessException("Got IO Exception when processing snippet", error, null);
        }
        ReadabilityResult result = processFile(tempSnippetFile.getPath(),attachMetrics);
        tempSnippetFile.delete();
        return result;
    }

    /**
     * Call the RSE.jar file with parameters to rate the file located
     * at a given file path
     * @param path the path pointing to the file to rate
     * @param attachMetrics whether to attach code metrics to the result object or not
     * @return the ReadabilityResult parsed from the RSE.jar output
     * @throws ReadabilityProcessException
     */
    private ReadabilityResult processFile(String path, boolean attachMetrics)  throws ReadabilityProcessException {

        ProcessBuilder rseCommand;

        try {
            rseCommand = createReadabilityCommand(path);
        }
        catch (URISyntaxException error) {
            throw new ReadabilityProcessException("Failed to create rse.jar command",error,path);
        }

        Process process;
        String stdout;
        String stderr;

        try {
            process = rseCommand.start();
            stderr = getBufferContent(process.getErrorStream());
            stdout = getBufferContent(process.getInputStream());
        }
        catch (IOException error) {
            throw new ReadabilityProcessException("Failed to execute rse.jar command",error,path);
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        }
        catch (InterruptedException error) {
            throw new ReadabilityProcessException("Got interrupted during processing ",error,path);
        }

        if (exitCode != 0) {
            String errorMsg = "rse command terminated with exit code " + exitCode +": " + stderr;
            throw new ReadabilityProcessException(errorMsg,null,path);
        }

        List<ReadabilityResult> resultList = ReadabilityResult.fromStdOut(stdout);

        if(!attachMetrics) {
            return resultList.get(0);
        }

        for(ReadabilityResult result: resultList) {
            try {
                String metricsStdout = processMetrics(result);
                ReadabilityResult.attachMetrics(result,metricsStdout);
            }
            catch (MetricsProcessException error) {
                throw new ReadabilityProcessException("Failed to extract metrics",error,result.getAnalyzedFile());
            }
        }
        return resultList.get(0);

    }

    /**
     * Calls the RSE.jat to extract metrics for a code snippet.
     * The path to the code snippet is taken from the provided ReadabilityResult object.
     *
     * The resulting model stdout is then returned.
     *
     * @param result the readability result of the code snippets the metrics should be extracted for.
     * @return the resulting model stdout
     * @throws MetricsProcessException if any issued appear during the extraction process
     */
    private String processMetrics(ReadabilityResult result) throws MetricsProcessException {
        ProcessBuilder metricsCommand;

        try {
            metricsCommand = createMetricsCommand(result);
        }
        catch (URISyntaxException error) {
            throw new MetricsProcessException("Failed to create metrics extract command",result,error);
        }

        Process process;
        String stdout;
        String stderr;

        try {
            process = metricsCommand.start();
            stderr = getBufferContent(process.getErrorStream());
            stdout = getBufferContent(process.getInputStream());
        }
        catch (IOException error) {
            throw new MetricsProcessException("Failed to execute metrics rse.jar command",result,error);
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        }
        catch (InterruptedException error) {
            throw new MetricsProcessException("Got interrupted during processing metrics",result,error);
        }

        if (exitCode != 0) {
            String errorMsg = "rse command terminated with exit code " + exitCode +": " + stderr;
            throw new MetricsProcessException(errorMsg,result,null);
        }
        return stdout;
    }

    /**
     * Convert an input stream buffer to a string by extracting its content.
     * @param input the input stream to extract the content from.
     * @return the extracted content.
     * @throws IOException if a error appears during the conversion process
     */
    private String getBufferContent(InputStream input) throws IOException {
        String bufferContent = "";

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(input));
        String nextLine;
        while ((nextLine = outputReader.readLine()) != null) {
            bufferContent += nextLine;
            bufferContent += "\n";
        }

        return bufferContent;
    }

    /**
     * Create the cli command to call the RSE.jar with a given code snippet.
     * The command will then if executed return the model readability by printing it to stdout.
     * @param codeFilePath the path pointing to the java file to get the readability for
     * @return A Process Builder Object that can be used to run the generated cli command.
     * @throws URISyntaxException if the given codeFilePath is invalid
     */
    private ProcessBuilder createReadabilityCommand(String codeFilePath) throws URISyntaxException {
        File codeFile = new File(codeFilePath);

        Path rseJarFolderPath = Paths.get(rseJarPath);
        String relativePath = rseJarFolderPath.relativize(Paths.get(codeFilePath)).toString();

        if (codeFile.isDirectory()) {
            relativePath += File.separator + "*.java";
        }

        ProcessBuilder readabilityCommand = new ProcessBuilder();

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            readabilityCommand.command("cmd", "/c", "cd", "/d", rseJarPath, "&&", "java", "-jar", "RSE.jar", relativePath);
        } else {
            readabilityCommand.command( "sh", "-c" ,"cd " + rseJarPath + " && java -jar RSE.jar "+ relativePath);
        }

        return readabilityCommand;
    }

    /**
     * Create the cli command to call the RSE.jar with a given code snippet.
     * The command will then if executed return the code metrics extracted from the code snippet.
     * The java files directory is taken from the provided ReadabilityResult object.
     *
     * @param result the path ReadabilityResult holding the path to the target java code snippet file
     * @return A Process Builder Object that can be used to run the generated cli command
     * @throws URISyntaxException if the codeFilePath taken from the ReadabilityResult object is invalid
     */
    private ProcessBuilder createMetricsCommand(ReadabilityResult result) throws URISyntaxException {

        String relativePath = result.getAnalyzedFile();

        ProcessBuilder metricsCommandBuilder = new ProcessBuilder();

        String metricsExtract = "it.unimol.readability.metric.runnable.ExtractMetrics";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            metricsCommandBuilder.command("cmd", "/c", "cd", "/d", rseJarPath, "&&", "java", "-cp", "RSE.jar",
                    metricsExtract, relativePath);
        } else {
            metricsCommandBuilder.command("sh","-c", "cd " + rseJarPath + " && java -cp RSE.jar " + metricsExtract + " " + relativePath);
        }

        return metricsCommandBuilder;
    }

}

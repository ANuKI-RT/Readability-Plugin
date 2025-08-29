package de.uni_passau.fim.readability_plugin.services;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import de.uni_passau.fim.readability_plugin.java_parsing.JavaFileParser;
import de.uni_passau.fim.readability_plugin.java_parsing.JavaParseTree;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityApi;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityProcessException;
import de.uni_passau.fim.readability_plugin.readability.ReadabilityResult;
import de.uni_passau.fim.readability_plugin.readability.ScalabrinoReadabilityApi;
import de.uni_passau.fim.readability_plugin.painting.RatedJavaScope;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This central service can be used to rate the readability of java code within the plugin.
 *
 * A complete java class with all methods can be rated.
 * If the methods a re-evaluated the service will automatically trigger the colouring process to render the
 * coloured gutter-icons representing a method's readability.
 *
 * In addition to rating and coloring a complete java file,
 * this service class also provides an api to rate a single code snippet.
 */
@Service(Service.Level.PROJECT)
public final class ReadabilityService {

    private final Project project;
    private ReadabilityApi api;

    private LinePainterService painterService;

    private Application app;

    private Map<String,Map<String,RatedJavaScope>> ratedMethodBuffer;

    private SettingsService settingsService;

    private MetaDataService metaDataService;

    ReadabilityService(Project project) {
        api = ScalabrinoReadabilityApi.getInstance();
        this.project = project;
        ratedMethodBuffer = new HashMap<>();
        painterService = project.getService(LinePainterService.class);
        metaDataService = project.getService(MetaDataService.class);
        settingsService = project.getService(SettingsService.class);
        app = ApplicationManager.getApplication();
    }

    /**
     * Updated the readability of a java files method.
     * The service does cache the old readability values of the method if it was rated before.
     * Therefore, a new rating will only be created if the content (or header) of the method did change.
     *
     * The methods are rated in parallel in order to increase the performance.
     *
     * After re-rating the readability values the coloring service is triggered to render the updated coloured gutter
     * icons
     *
     * @param file the java file containing the java method to re-rate and re-colour
     * @throws ReadabilityProcessException if there appeared an error during the re-rating process
     */
    public void updateReadability(VirtualFile file) throws ReadabilityProcessException {


        String taskTitle = settingsService.showUi() ? "Calculating Readability for file " + file.getName() :
                "Preparing " + file.getName();
        Task.Backgroundable task = new Task.Backgroundable(project, taskTitle) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {

                    JavaFileParser fileParser = new JavaFileParser(project, file);
                    List<JavaParseTree> methods = fileParser.getMethods();

                    if (methods.size() == 0) {
                        return;
                    }

                    List<RatedJavaScope> ratedMethods = new ArrayList<>();

                    int numThreads = Math.min(methods.size(), Runtime.getRuntime().availableProcessors());
                    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                    List<Future<RatedJavaScope>> futures = new ArrayList<>();

                    indicator.setIndeterminate(false);
                    indicator.setFraction(0);

                    Map<String,RatedJavaScope> methodBuffer = ratedMethodBuffer.getOrDefault(file.getPath(),new HashMap<>());

                    for (int i = 0; i < methods.size(); i++) {
                        Callable<RatedJavaScope> callable = new ReadabilityProcessor(methods.get(i), api,methodBuffer);
                        futures.add(executor.submit(callable));
                        indicator.setFraction((double) i / methods.size());
                    }


                    Map<String,RatedJavaScope> updatedBuffer = new HashMap<>();
                    for (Future<RatedJavaScope> future : futures) {
                        try {
                            RatedJavaScope ratedMethod = future.get();
                            ratedMethods.add(ratedMethod);
                            updatedBuffer.put(ratedMethod.getCode(), ratedMethod);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    executor.shutdown();
                    ratedMethodBuffer.put(file.getPath(),updatedBuffer);
                    if(!ratedMethods.isEmpty() && settingsService.showUi()) {
                        SwingUtilities.invokeLater(() -> {
                            painterService.colorLines(file, ratedMethods);
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        ProgressManager.getInstance().run(task);
    }

    /**
     * Rates a single code snippet and provides it readability.
     * Please note that when using this method no code metrics are extracted.
     *
     * @param codeSnippet the code snippet to rate
     * @param linkedElement the parse tree element matching the code that should be rated
     * @return
     * @throws ReadabilityProcessException if there appeared an error during the rating process
     */
    public ReadabilityResult rateSnippet(String codeSnippet, PsiElement linkedElement) throws ReadabilityProcessException {
        return api.processCodeSnippet(codeSnippet, false, linkedElement);
    }

    /**
     * Utility class to create a Callable for providing the readability of a snippet.
     * This class is required in order to rate the different snippets in parallel.
     *
     * If the Processor is called for a given method it will provide its readability.
     */
    private class ReadabilityProcessor implements Callable<RatedJavaScope> {
        private JavaParseTree javaMethod;
        private ReadabilityApi api;
        private Map<String,RatedJavaScope> methodBuffer;

        public ReadabilityProcessor(JavaParseTree method, ReadabilityApi api, Map<String,RatedJavaScope> methodBuffer ) {
            this.javaMethod = method;
            this.api = api;
            this.methodBuffer = methodBuffer;
        }

        /**
         * Provides the readability of the java method referenced in the javaMethod class attribute.
         *
         * First checks if the methods content did change.
         * If it did the method is re-rated and the result is added to the cache (and returned).
         * If the content did not change the cached readability value in simply returned.
         * @return the readability result for the given method
         * @throws Exception if any error appeared during the rating process.
         */
        @Override
        public RatedJavaScope call() throws Exception {

            String methodContent = javaMethod.getCode();
            if(methodBuffer.containsKey(methodContent)) {
                RatedJavaScope bufferedRating = methodBuffer.get(methodContent);
                //if 2 methods are completely the same the buffer matches 2 methods
                //therefore not returning the buffer content but only the readability result
                //of the buffer + the javaMethod that was actually rated
                return new RatedJavaScope(javaMethod,bufferedRating.getReadabilityResult());
            }

            ReadabilityResult result = api.processCodeSnippet(methodContent, true, javaMethod.getScopeElement());
            RatedJavaScope newRating = new RatedJavaScope(javaMethod, result);
            metaDataService.registerReadabilityReRender(newRating);
            return newRating;
        }
    }
}

package de.uni_passau.fim.readability_plugin.readability;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used to represent the result of a readability assessment within the plugin.
 * It can be used to retrieve the readability score and (if the readability model was metric based) the
 * readability metrics that were used to calculate it.
 */
public class ReadabilityResult {

    private final String analyzedFile;
    private final double readability;

    private Map<String,Double> metrics;

    /**
     * !!! THIS METHOD MUST BE REFACTORED AN IMPLEMENTED AT ANOTHER PLACE !!!
     *
     * This method does take the stdout string of the Scalabrino Readability Model from 2018
     * and parses the string to extract the readability score. As the model can in theory take
     * multiple java files at once, the created ReadabilityResult objects are returned as a list.
     *
     *
     * The ReadabilityResult should be abstract and contain readability values created by different models.
     * Therefore, this method must be refactoed and put in a implementation of ReadabilityResult that is specifically
     * defined for the Scalabrino Model
     *
     * TODO: Place method at another Scalabrino specific class
     *
     * @param stdout the stdout of the model to parse
     * @return the created ReadabilityResult object generated from the parsed output
     */
    public static List<ReadabilityResult> fromStdOut(String stdout) {
        List<String> stdoutLines = Arrays.asList(stdout.split("\n"));

        List<String> fileResultLines = stdoutLines
                        .stream().filter(line -> !line.startsWith("[INFO]") && !line.startsWith("file"))
                        .collect(Collectors.toList());

        List<ReadabilityResult> resultList = new ArrayList<>();
        for(String resultLine: fileResultLines) {
            String[] resultParts = resultLine.split("\t| ");
            String file = resultParts[0];
            double readability = Double.parseDouble(resultParts[1]);
            resultList.add(new ReadabilityResult(file,readability));
        }
        return resultList;
    }

    /**
     * !!! THIS METHOD MUST BE REFACTORED AN IMPLEMENTED AT ANOTHER PLACE !!!
     *
     * This method does take the stdout string of the Scalabrino Readability Model from 2018
     * and parses the string to extract the readability metrics used to generate the score.
     * The metric values are then appended to a provided ReadabilityResult object.
     *
     * The ReadabilityResult should be abstract and contain readability values created by different models.
     * Therefore, this method must be refactoed and put in a implementation of ReadabilityResult that is specifically
     * defined for the Scalabrino Model
     *
     * TODO: Place method at another Scalabrino specific class
     *
     * @param result the readability results the metrics should be appended to
     * @param metricsStdOut the metrics stdout of the model to parse
     * @return the created ReadabilityResult object generated from the parsed output
     */
    public static void attachMetrics(ReadabilityResult result, String metricsStdOut) {
        List<String> stdoutLines = Arrays.asList(metricsStdOut.split("\n"));

        List<String> metricsLines = stdoutLines
                .stream().filter(line -> !line.startsWith("[INFO]") && !line.startsWith("file"))
                .collect(Collectors.toList());

        Map<String,Double> metrics = new HashMap();
        for(String metric: metricsLines) {
            String[] metricParts = metric.split(": ");
            String name = metricParts[0];
            double value = Double.parseDouble(metricParts[1]);
            metrics.put(name,value);
        }

        result.metrics = metrics;
    }

    /**
     * Create a new Readability result
     * @param analyzedFile the java file containing the code analyzed by the model
     * @param readability the calculated readability value
     */
    private ReadabilityResult(String analyzedFile, double readability) {
        this(analyzedFile,readability,null);
    }

    /**
     * Create a new Readability result and instantly pass metric values used to generate the readability score
     * @param analyzedFile the java file containing the code analyzed by the model
     * @param readability the calculated readability value
     * @param metrics the code metrics used to generate the readability score
     */
    private ReadabilityResult(String analyzedFile, double readability, Map<String,Double> metrics) {
        this.analyzedFile = analyzedFile;
        this.readability = readability;
    }

    /**
     * Get the readability score calculated by the model
     * @return the readability score
     */
    public double getReadability() {
        return readability;
    }

    /**
     * Get the directory path pointing to the analyzed java file
     * @return the directory path
     */
    public String getAnalyzedFile() {
        return analyzedFile;
    }

    /**
     * Returns true if the readability result has metrics attached
     * @return whether the readability result has metrics attached or not
     */
    public boolean hasMetricsAttached() {
        return metrics != null;
    }

    /**
     * Get the metrics attached to the readability result object.
     * @return the attached metrics or null of no metrics were attached.
     */
    public Map<String,Double> getMetrics() {
        return metrics;
    }

    /**
     * Get a specific metric attached to the readability result.
     * @param metricName the name of the metric
     * @return the metric value or null if no metrics were attached or the provided metric name does not exists
     */
    public Double getMetric(String metricName) {
        if(metrics == null) {
            return null;
        }
        return metrics.get(metricName);
    }

    /**
     * Set the directory path and the readability score as string representation of the object.
     * @return the string representation of the ReadabilityResult
     */
    @Override
    public String toString() {
        return analyzedFile + " " + readability;
    }
}

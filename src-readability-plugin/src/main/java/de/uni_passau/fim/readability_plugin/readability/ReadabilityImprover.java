package de.uni_passau.fim.readability_plugin.readability;

import de.uni_passau.fim.readability_plugin.painting.RatedJavaScope;

import java.util.*;

/**
 * For the readability hints feature, metrics that have a very high influence on the readability score
 * must be determined.
 *
 * For the determined metrics, help text can then be rendered.
 * This class implements an api to find the metrics that should be used to improve the readability of the code,
 * by comparing the codes metrics with metrics that generally represent well readable code.
 */
public class ReadabilityImprover {

    private RatedJavaScope ratedMethod;

    /**
     * The java method that should be improved in readability, by improving some metrics
     * must be passed in the constructor by providing a RatedJavaScope instance representing the methods
     * scope withing the java classes parse tree.
     * @param ratedMethod the RatedJavaScope associated with the rated method.
     */
    public ReadabilityImprover(RatedJavaScope ratedMethod) {
        this.ratedMethod = ratedMethod;
    }

    /**
     * Determined the metrics that must be improved in order to get a maximum
     * increase in readability, and returns an iterator for the found metrics.
     * @return the Iterator including the Improvement objects for the different metrics.
     */
    public Iterator<Improvement> improve() {
        ReadabilityResult result = ratedMethod.getReadabilityResult();

        ImprovementRanking ranking = new ImprovementRanking();
        Map<String,Double> resultMetrics = result.getMetrics();
        Map<String,Double> meanImprovedMetrics = getMeanImprovementMetrics();
        double initialReadability = runRegression(resultMetrics);
        for (Map.Entry<String, Double> entry : meanImprovedMetrics.entrySet()) {
            String improvedMetricName = entry.getKey();
            Double improvedMetricValue = entry.getValue();
            Map<String,Double> improvedMetrics = createImprovedMetrics(resultMetrics,improvedMetricName,improvedMetricValue);
            Double improvedReadability = runRegression(improvedMetrics);
            if(improvedReadability > initialReadability) {
                Map<String,Double> improveParameters = new HashMap<>();
                improveParameters.put("improvedMetricValue",improvedMetricValue);
                improveParameters.put("actualMetricValue",result.getMetric(improvedMetricName));
                improveParameters.put("improvedReadability",improvedReadability);
                improveParameters.put("actualReadability",initialReadability);
                ranking.addMetric(improvedMetricName,improveParameters);
            }
        }
        return ranking.rank();
    }

    /**
     * Run a logical regression using given metric values in order to
     * generate a readability score.
     * @param metrics the metric values to use
     * @return the calculated readability score
     */
    private double runRegression(Map<String, Double> metrics) {
        Map<String, Double> coefficients = getCoefficients();
        double intercept = getIntercept();

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            String key = entry.getKey();
            if(coefficients.containsKey(key)) {
                double metricValue = entry.getValue();
                double coefficientValue = coefficients.get(key);
                dotProduct += metricValue * coefficientValue;
            }
        }

        double linearCombination = dotProduct + intercept;

        double logisticRegressionOutput = 1 / (1 + Math.exp(-linearCombination));

        return 1 - logisticRegressionOutput;

    }

    /**
     * In order to determine how big the influence of a specific metric is on a codes readability,
     * the readability actual readability score (created when using this metric value) must be compared to a
     * readability score created by the same metrics except the one to determine the readability influence for.
     * This metric must be exchanged with a metric value that represents well readable code in order to compare the 2
     * readability values generated using the metrics.
     *
     * This method provides a copy of the given metric value map.
     * The only change applied to the mapping is exchanging a specific given metric with another value representing
     * well readable code
     * @param base the mapping to create a modified copy for
     * @param metricKey the metric to overwrite
     * @param metricValue the value to overwrite the metric with
     * @return the modified metric mapping with the exchanged metric
     */
    private Map<String,Double>  createImprovedMetrics(Map<String,Double> base,String metricKey, Double metricValue) {
        Map<String,Double> improved = new HashMap<>();
        for (Map.Entry<String, Double> entry : base.entrySet()) {
            improved.put(entry.getKey(),entry.getValue());
        }
        improved.put(metricKey,metricValue);
        return improved;
    }

    /**
     * Provide the coefficient values used for the logical regression.
     *
     * TODO: those coefficients should be configurable in a external config file
     * @return the coefficients for the different metrics
     */
    private Map<String, Double> getCoefficients() {
        Map<String, Double> result = new HashMap<>();
        result.put("New Commented words MAX", 0.1106);
        result.put("New Synonym commented words MAX", -0.0625);
        result.put("New Text Coherence MAX", 0.5508);
        result.put("BW Avg comparisons", 1.1061);
        result.put("BW Avg numbers", 0.7925);
        result.put("BW Avg parenthesis", 0.129);
        result.put("BW Max line length", 0.0106);
        result.put("BW Max number of identifiers", 0.054);
        result.put("BW Max numbers", -0.0941);
        result.put("Posnett volume", 0.0023);
        result.put("Dorn DFT Commas", -0.0316);
        result.put("Dorn DFT Comparisons", -0.0014);
        result.put("Dorn DFT Keywords", 0.0291);
        result.put("Dorn DFT LineLengths", -0.032);
        result.put("Dorn DFT Periods", -0.0332);
        result.put("Dorn DFT Spaces", -0.0344);
        result.put("Dorn Visual Y Comments", -0.0475);
        result.put("Dorn Visual Y Identifiers", 0.1542);
        result.put("Dorn Visual Y Keywords", -0.065);
        result.put("Dorn Visual Y Numbers", 0.0092);
        result.put("Dorn Areas Comments", -0.1018);
        result.put("Dorn Areas Identifiers", 3.151);
        result.put("Dorn Areas Keywords/Identifiers", -1.4795);
        result.put("Dorn align blocks", -0.0092);
        return result;
    }

    /**
     * Provide the metric values that generally represent well readable code.
     *
     * TODO: those metric values should be configurable in a external config file
     * @return the metrics values representing well readable code
     */
    private Map<String,Double> getMeanImprovementMetrics() {
        Map result = new HashMap<String,Double>();
        result.put("New Commented words MAX", 1.8666666666666667);
        result.put("New Synonym commented words MAX", 12.08);
        result.put("New Text Coherence MAX", 0.3471257128631813);
        result.put("BW Avg comparisons", 0.0486999320282494);
        result.put("BW Avg numbers", 0.16816856124271212);
        result.put("BW Avg parenthesis", 0.8511882778236841);
        result.put("BW Max line length", 69.48);
        result.put("BW Max number of identifiers", 5.906666666666666);
        result.put("BW Max numbers", 1.5066666666666666);
        result.put("Posnett volume", 476.6499244890177);
        result.put("Dorn DFT Commas", 10.973333333333333);
        result.put("Dorn DFT Comparisons", 9.306666666666667);
        result.put("Dorn DFT Keywords", 16.36);
        result.put("Dorn DFT LineLengths", 18.933333333333334);
        result.put("Dorn DFT Periods", 16.613333333333333);
        result.put("Dorn DFT Spaces", 14.213333333333333);
        result.put("Dorn Areas Comments", 0.21825237213856463);
        result.put("Dorn Areas Identifiers", 0.37966093761625824);
        result.put("Dorn Areas Keywords/Identifiers", 0.23394542257613102);
        result.put("Dorn align blocks", 32.44);
        return result;
    }

    /**
     * Provide the intercept used in the logical regression
     * TODO: this intercept should be configurable in a external config file
     * @return the intercept value
     */
    private double getIntercept() {
        return -3.8428;
    }

    /**
     * Ths class checks for every code metric how big the influence in improving readability is,
     * if this metric is improved. It then sorts those improvement objects from most to least influence.
     * The ranked values are then represented as an iterator in order to loop over them in the calculated ranking
     * order.
     */
   private class ImprovementRanking implements Iterator<Improvement> {

        private int pointer;
        List<Improvement> rankedMetrics;
        private ImprovementRanking() {
            pointer = 0;
            rankedMetrics = new ArrayList<>();
        }

        /**
         * Add a metric and its improvement values to the ranking.
         * @param metric the metric to add
         * @param improveParameters the readability-improvements that can be made when improving the metric
         */
        private void addMetric(String metric, Map<String,Double> improveParameters) {
            rankedMetrics.add(new Improvement(metric,improveParameters));
        }

        @Override
        public boolean hasNext() {
           return pointer < rankedMetrics.size();
        }

        @Override
        public Improvement next() {
           Improvement next = rankedMetrics.get(pointer);
           pointer++;
           return next;
        }

        /**
         * Ranks the metrics by the readability increase they bring when improving them,
         * and returns an iterator to loop over the ranked metric, by providing a ImprovementRanking instance-
         * @return the calculated improvement ranking
         */
        private ImprovementRanking rank() {
            rankedMetrics.sort((o1, o2) -> {
               Double o1Improvement = o1.improveParameters.get("improvedReadability");
               Double o2Improvement = o2.improveParameters.get("improvedReadability");

               if(o1Improvement < o2Improvement) {
                   return 1;
               }
               if(o1Improvement > o2Improvement) {
                   return -1;
               }
               return  0;
            });

            for (int i = 0; i < rankedMetrics.size(); i++) {
                rankedMetrics.get(i).ranking = i+1;
            }
            return this;
        }

   }

    /**
     * Wrapper class to represent a metric improvement.
     * An Improvement object contains the actual readability value
     * if the actual metric is used to generate the score and the readability value that is generated
     * when using the improved metrics. In addition to that the actual metric values used when calculating the score
     * can be accessed.
     */
   public class Improvement {
        private String metric;
        private int ranking;

        private String targetMethod;

        private Map<String,Double> improveParameters;

        /**
         * Create an improvement object by passing the metric name and the metric/readability values
         * associated to it
         * @param metric the metric name
         * @param improveParameters the metric/readability values
         */
        private Improvement(String metric, Map<String,Double> improveParameters) {
            this.metric = metric;
            this.ranking = -1;
            this.improveParameters = improveParameters;
        }

        /**
         * get the ranking of the improvement within the ReadabilityRanking
         * @return the ranking
         */
        public int getRanking() {
            return ranking;
        }

        /**
         * Get the name of the metric that should be improved
         * @return the metric name
         */
        public String getMetric() {
            return metric;
        }

        /**
         * Get the improved metric value
         * @return the improved metric value
         */
        public Double getImprovedMetricValue() {
            return improveParameters.get("improvedMetricValue");
        }

        /**
         * Get the actual metric value
         * @return the actual metric value
         */
        public Double getActualMetricValue() {
            return improveParameters.get("actualMetricValue");
        }

        /**
         * Get the improved readability value
         * @return the improved readability value
         */
        public Double getImprovedReadability() {
            return improveParameters.get("improvedReadability");
        }

        /**
         * Get the actual readability value
         * @return the actual readability value
         */
        public Double getActualReadability() {
            return improveParameters.get("actualReadability");
        }

        /**
         * Calculates the difference between the actual readability and the readability using the
         * improved metrics and returns this difference.
         * @return the improvement value represented by the readability difference
         */
        public Double getReadabilityImprovement() {
            return getImprovedReadability() - getActualReadability();
        }
   }




}

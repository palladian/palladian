/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class FeatureSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSelector.class);

    /**
     * <p>
     * This is a static utility class. The constructor should never be called.
     * </p>
     */
    private FeatureSelector() {
        throw new UnsupportedOperationException("Unable to instantiate static utility class");
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param featurePath
     * @param featureType
     * @param instances
     * @return
     */
    public static <T extends Feature<?>> Map<String, Map<String, Double>> calculateChiSquareValues(String featurePath,
            Class<T> featureType, Collection<Instance> instances) {
        Map<String, Map<String, Integer>> termClassCorrelationMatrix = new HashMap<String, Map<String, Integer>>();
        Map<String, Integer> classCounts = new HashMap<String, Integer>();
        Map<String, Map<String, Double>> ret = new HashMap<String, Map<String, Double>>();

        for (Instance instance : instances) {
            Collection<T> features = instance.featureVector.getFeatureBag(featureType, featurePath);
            for (T value : features) {
                addCooccurence(value.getValue().toString(), instance.targetClass, termClassCorrelationMatrix);
            }
            Integer count = classCounts.get(instance.targetClass);
            if (count == null) {
                count = 0;
            }
            classCounts.put(instance.targetClass, ++count);
        }

        // The following variables are uppercase because that is the way they are used in the literature.
        for (Map.Entry<String, Map<String, Integer>> termOccurence : termClassCorrelationMatrix.entrySet()) {
            int N = instances.size();
            for (Map.Entry<String, Integer> currentClassCount : classCounts.entrySet()) {
                // for (Map.Entry<String, Integer> classOccurence : termOccurence.getValue().entrySet()) {
                String className = currentClassCount.getKey();
                Integer classCount = currentClassCount.getValue();
                Integer termClassCoocurrence = termOccurence.getValue().get(className);
                if (termClassCoocurrence == null) {
                    termClassCoocurrence = 0;
                }
                LOGGER.debug("Calculating Chi² for feature {} in class {}.", termOccurence.getKey(), className);
                int N_11 = termClassCoocurrence;
                int N_10 = sumOfRowExceptOne(termOccurence.getKey(), className, termClassCorrelationMatrix);
                int N_01 = classCount - termClassCoocurrence;
                int N_00 = N - (N_10 + N_01 + N_11);
                LOGGER.debug("Using N_11 {}, N_10 {}, N_01 {}, N_00 {}", new Integer[] {N_11, N_10, N_01, N_00});

                double E_11 = calculateExpectedCount(N_11, N_10, N_11, N_01, N);
                double E_10 = calculateExpectedCount(N_11, N_10, N_00, N_10, N);
                double E_01 = calculateExpectedCount(N_01, N_00, N_11, N_01, N);
                double E_00 = calculateExpectedCount(N_01, N_00, N_10, N_00, N);

                double chiSquare = Double.valueOf(N_11 + N_10 + N_01 + N_00) * Math.pow(N_11 * N_00 - N_10 * N_01, 2)
                        / ((N_11 + N_01) * (N_11 + N_10) * (N_10 + N_00) * (N_01 + N_00));
                // double chiSquare = summand(N_11, E_11) + summand(N_10, E_10) + summand(N_01, E_01)
                // + summand(N_00, E_00);

                LOGGER.debug("Chi² value is {}", chiSquare);
                Map<String, Double> chiSquaresForCurrentTerm = ret.get(termOccurence.getKey());
                if (chiSquaresForCurrentTerm == null) {
                    chiSquaresForCurrentTerm = new HashMap<String, Double>();
                }
                chiSquaresForCurrentTerm.put(className, chiSquare);
                ret.put(termOccurence.getKey(), chiSquaresForCurrentTerm);
            }
        }

        return ret;
    }

    // /**
    // * <p>
    // *
    // * </p>
    // *
    // * @param n_11
    // * @param e_11
    // * @return
    // */
    // private static double summand(Integer n, double e) {
    // return Math.pow(n.doubleValue() - e, 2) / e;
    // }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param n_11
     * @param n_10
     * @param n_112
     * @param n_01
     * @return
     */
    private static double calculateExpectedCount(Integer first, Integer second, Integer third, Integer fourth, Integer N) {
        return N.doubleValue() * (first.doubleValue() + second.doubleValue()) / N.doubleValue()
                * (third.doubleValue() + fourth.doubleValue()) / N.doubleValue();
    }

    /**
     * <p>
     * Sums up a row of a matrix leaving one column out. This is required to calculate N01 and N10 for Chi² test. N01 is
     * the amount of documents of a class without a certain term, while N10 is the amount of documents with a certain
     * term but not of the specified class.
     * </p>
     * 
     * @param rowValue The value of the row to create the sum for.
     * @param exception The column to leave out of the summation.
     * @param correlationMatrix A matrix where cells are the counts of
     *            how often a the row value and the column value occur together.
     * @return The sum of the class occurrence without the term.
     */
    private static int sumOfRowExceptOne(String rowValue, String exception,
            Map<String, Map<String, Integer>> correlationMatrix) {
        Map<String, Integer> occurencesOfClass = correlationMatrix.get(rowValue);
        // add up all occurences of the current class
        int ret = 0;
        for (Map.Entry<String, Integer> occurence : occurencesOfClass.entrySet()) {
            if (!occurence.getKey().equals(exception)) {
                ret += occurence.getValue();
            }
        }
        return ret;
    }

    private static void addCooccurence(String row, String column, Map<String, Map<String, Integer>> correlationMatrix) {
        Map<String, Integer> correlations = correlationMatrix.get(row);
        if (correlations == null) {
            correlations = new HashMap<String, Integer>();
        }
        Integer occurenceCount = correlations.get(column);
        if (occurenceCount == null) {
            occurenceCount = 0;
        }
        occurenceCount++;
        correlations.put(column, occurenceCount);
        correlationMatrix.put(row, correlations);

    }

    public static void main(String[] args) {
        FeatureVector fv1 = new FeatureVector();
        FeatureVector fv2 = new FeatureVector();
        FeatureVector fv3 = new FeatureVector();

        fv1.add(new NominalFeature("testfeature", "a"));
        fv1.add(new NominalFeature("testfeature", "b"));
        fv1.add(new NominalFeature("testfeature", "c"));
        fv1.add(new NominalFeature("testfeature", "a"));
        fv1.add(new NominalFeature("testfeature", "d"));

        fv2.add(new NominalFeature("testfeature", "a"));
        fv2.add(new NominalFeature("testfeature", "b"));
        fv2.add(new NominalFeature("testfeature", "c"));

        fv3.add(new NominalFeature("testfeature", "d"));
        fv3.add(new NominalFeature("testfeature", "e"));
        fv3.add(new NominalFeature("testfeature", "f"));

        Instance instance1 = new Instance();
        instance1.featureVector = fv1;
        instance1.targetClass = "c1";

        Instance instance2 = new Instance();
        instance2.featureVector = fv2;
        instance2.targetClass = "c1";

        Instance instance3 = new Instance();
        instance3.featureVector = fv3;
        instance3.targetClass = "c2";

        Collection<Instance> instances = new HashSet<Instance>();
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);

        Map<String, Map<String, Double>> chiSquareValues = calculateChiSquareValues("testfeature",
                NominalFeature.class, instances);
        System.out.println(chiSquareValues);
    }
}

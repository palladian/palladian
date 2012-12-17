/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureUtils;
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
/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
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
    public static <T> Map<String, Map<String, Double>> calculateChiSquareValues(String featurePath,
            Class<? extends Feature<T>> featureType, Collection<Instance> instances) {
        Map<String, Map<String, Long>> termClassCorrelationMatrix = new HashMap<String, Map<String, Long>>();
        Map<String, Long> classCounts = new HashMap<String, Long>();
        Map<String, Map<String, Double>> ret = new HashMap<String, Map<String, Double>>();

        for (Instance instance : instances) {
            Collection<? extends Feature<T>> features = FeatureUtils.convertToSet(instance.getFeatureVector(),
                    featureType, featurePath);
            // XXX changed, untested -- 2012-11-17 -- Philipp
            // XXX needs to be changed back since this allows duplicates in the Collection of features which causes
            // errors -- 2012-11-20 -- Klemens
            // Collection<? extends Feature<T>> features = instance.getFeatureVector().getAll(featureType, featurePath);
            for (Feature<T> value : features) {
                addCooccurence(value.getValue().toString(), instance.getTargetClass(), termClassCorrelationMatrix);
            }
            Long count = classCounts.get(instance.getTargetClass());
            if (count == null) {
                count = 0L;
            }
            classCounts.put(instance.getTargetClass(), ++count);
        }

        // The following variables are uppercase because that is the way they are used in the literature.
        for (Map.Entry<String, Map<String, Long>> termOccurence : termClassCorrelationMatrix.entrySet()) {
            int N = instances.size();
            for (Map.Entry<String, Long> currentClassCount : classCounts.entrySet()) {
                // for (Map.Entry<String, Integer> classOccurence : termOccurence.getValue().entrySet()) {
                String className = currentClassCount.getKey();
                Long classCount = currentClassCount.getValue();
                Long termClassCoocurrence = termOccurence.getValue().get(className);
                if (termClassCoocurrence == null) {
                    termClassCoocurrence = 0L;
                }
                LOGGER.debug("Calculating Chi² for feature {} in class {}.", termOccurence.getKey(), className);
                long N_11 = termClassCoocurrence;
                long N_10 = sumOfRowExceptOne(termOccurence.getKey(), className, termClassCorrelationMatrix);
                long N_01 = classCount - termClassCoocurrence;
                long N_00 = N - (N_10 + N_01 + N_11);
                LOGGER.debug("Using N_11 {}, N_10 {}, N_01 {}, N_00 {}", new Long[] {N_11, N_10, N_01, N_00});

                // double E_11 = calculateExpectedCount(N_11, N_10, N_11, N_01, N);
                // double E_10 = calculateExpectedCount(N_11, N_10, N_00, N_10, N);
                // double E_01 = calculateExpectedCount(N_01, N_00, N_11, N_01, N);
                // double E_00 = calculateExpectedCount(N_01, N_00, N_10, N_00, N);

                double numerator = Double.valueOf(N_11 + N_10 + N_01 + N_00) * Math.pow(N_11 * N_00 - N_10 * N_01, 2);
                long denominatorInt = (N_11 + N_01) * (N_11 + N_10) * (N_10 + N_00) * (N_01 + N_00);
                double denominator = Double.valueOf(denominatorInt);
                double chiSquare = numerator / denominator;
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
    private static long sumOfRowExceptOne(String rowValue, String exception,
            Map<String, Map<String, Long>> correlationMatrix) {
        Map<String, Long> occurencesOfClass = correlationMatrix.get(rowValue);
        // add up all occurences of the current class
        long ret = 0;
        for (Map.Entry<String, Long> occurence : occurencesOfClass.entrySet()) {
            if (!occurence.getKey().equals(exception)) {
                ret += occurence.getValue();
            }
        }
        return ret;
    }

    private static void addCooccurence(String row, String column, Map<String, Map<String, Long>> correlationMatrix) {

        Map<String, Long> correlations = correlationMatrix.get(row);
        if (correlations == null) {
            correlations = new HashMap<String, Long>();
        }
        Long occurenceCount = correlations.get(column);
        if (occurenceCount == null) {
            occurenceCount = 0L;
        }
        occurenceCount++;
        correlations.put(column, occurenceCount);
        correlationMatrix.put(row, correlations);

    }

    /**
     * <p>
     * G(t) = - sum^m_i=1 Pr(ci)log Pr(ci) + Pr(t) sum^m_i=1 Pr(ci|t) log Pr(ci|t) + Pr(!t) sum^m_i=1 pr(ci|!t) log
     * pr(ci|!t)
     * </p>
     * 
     * @param featurePath
     * @param dataset
     * @return
     */
    public static <T extends NominalFeature> Map<T, Double> calculateInformationGain(String featurePath,
            Collection<Instance> dataset) {
        Map<T, Double> ret = new HashMap<T, Double>();
        Map<String, Integer> classCounts = new HashMap<String, Integer>();
        Map<String, Integer> featuresInClass = new HashMap<String, Integer>();
        Integer sumOfFeaturesInAllClasses = 0;
        Map<T, Integer> absoluteOccurences = new HashMap<T, Integer>();
        Map<T, Map<String, Integer>> absoluteConditionalOccurences = new HashMap<T, Map<String, Integer>>();
        for (Instance instance : dataset) {
            Integer targetClassCount = classCounts.get(instance.getTargetClass());
            if (targetClassCount == null) {
                classCounts.put(instance.getTargetClass(), 1);
            } else {
                classCounts.put(instance.getTargetClass(), ++targetClassCount);
            }

            List<T> featuresList = FeatureUtils.getFeaturesAtPath(instance.getFeatureVector(), featurePath);
            Set<T> features = new HashSet<T>(featuresList); // remove duplicates
            for (T feature : features) {
                Integer countOfFeaturesInClass = featuresInClass.get(instance.getTargetClass());
                if (countOfFeaturesInClass == null) {
                    countOfFeaturesInClass = 0;
                }
                featuresInClass.put(instance.getTargetClass(), ++countOfFeaturesInClass);
                sumOfFeaturesInAllClasses++;

                Integer absoluteOccurence = absoluteOccurences.get(feature);
                if (absoluteOccurence == null) {
                    absoluteOccurences.put(feature, 1);
                } else {
                    absoluteOccurences.put(feature, ++absoluteOccurence);
                }

                Map<String, Integer> absoluteConditionalOccurence = absoluteConditionalOccurences.get(feature);
                if (absoluteConditionalOccurence == null) {
                    absoluteConditionalOccurence = new HashMap<String, Integer>();
                    absoluteConditionalOccurence.put(instance.getTargetClass(), 1);
                } else {
                    Integer occurenceInTargetClass = absoluteConditionalOccurence.get(instance.getTargetClass());
                    if (occurenceInTargetClass == null) {
                        absoluteConditionalOccurence.put(instance.getTargetClass(), 1);
                    } else {
                        absoluteConditionalOccurence.put(instance.getTargetClass(), ++occurenceInTargetClass);
                    }
                }
                absoluteConditionalOccurences.put(feature, absoluteConditionalOccurence);
            }
        }

        double classProb = 0.0d;
        for (Entry<String, Integer> classCount : classCounts.entrySet()) {
            double Prci = classCount.getValue().doubleValue() / dataset.size();
            classProb += Prci * Math.log(Prci);
        }

        for (Entry<T, Integer> absoluteOccurence : absoluteOccurences.entrySet()) {
            double G = 0.0d;

            double termClassCoocurrence = 0.0d;
            double termClassNonCoocurrence = 0.0d;
            for (Entry<String, Integer> absoluteConditionalOccurence : absoluteConditionalOccurences.get(
                    absoluteOccurence.getKey()).entrySet()) {
                int classCount = classCounts.get(absoluteConditionalOccurence.getKey());
                // Probability for class ci containing term t
                double Prcit = laplaceSmooth(absoluteOccurences.keySet().size(),
                        featuresInClass.get(absoluteConditionalOccurence.getKey()),
                        absoluteConditionalOccurence.getValue());
                termClassCoocurrence += Prcit * Math.log(Prcit);

                // Probability for class ci not containing term t
                double Prcint = laplaceSmooth(absoluteOccurences.keySet().size(), sumOfFeaturesInAllClasses
                        - featuresInClass.get(absoluteConditionalOccurence.getKey()), dataset.size()
                        - absoluteConditionalOccurence.getValue());
                ;
                termClassNonCoocurrence = Prcint * Math.log(Prcint);
            }
            double termProb = absoluteOccurence.getValue().doubleValue() / dataset.size() + termClassCoocurrence;

            double nonTermProb = ((double)(dataset.size() - absoluteOccurence.getValue())) / dataset.size()
                    + termClassNonCoocurrence;

            G = -classProb + termProb + nonTermProb;
            ret.put(absoluteOccurence.getKey(), G);
        }
        return ret;
    }

    private static double laplaceSmooth(int vocabularySize, int countOfFeature, int countOfCoocurence) {
        return (1.0d + ((double)countOfCoocurence)) / (((double)vocabularySize) + ((double)countOfFeature));
    }
}

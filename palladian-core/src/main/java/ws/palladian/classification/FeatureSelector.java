/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureUtils;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Utility class which provides methods to use basic feature selection strategies. Currently it provides Chi² test for
 * nominal features and Information Gain scoring
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class FeatureSelector {

    /**
     * <p>
     * 
     * </p>
     * 
     * @author Klemens Muthmann
     * @version 1.0
     * @since 0.2.0
     */
    private static final class Binner {
        private final List<NumericBin> bins;

        public Binner(String binName, List<Integer> boundaryPoints, List<NumericFeature> features) {
            bins = new ArrayList<FeatureSelector.NumericBin>(boundaryPoints.size() + 1);
            for (int i = 0; i < boundaryPoints.size(); i++) {
                double lowerBound = i == 0 ? features.get(0).getValue() : features.get(boundaryPoints.get(i - 1))
                        .getValue();
                double upperBound = features.get(boundaryPoints.get(i)).getValue();
                bins.add(new NumericBin(binName, lowerBound, upperBound, (double)i));
            }
            bins.add(new NumericBin(binName, features.get(boundaryPoints.get(boundaryPoints.size() - 1)).getValue(),
                    features.get(features.size() - 1).getValue(), (double)boundaryPoints.size()));
        }

        public List<NumericBin> bin(List<NumericFeature> features) {
            List<NumericBin> ret = new ArrayList<FeatureSelector.NumericBin>(features.size());
            for (NumericFeature feature : features) {
                int binPosition = Collections.binarySearch(bins, feature, new Comparator<NumericFeature>() {

                    @Override
                    public int compare(NumericFeature bin, NumericFeature feature) {
                        NumericBin numericBin = (NumericBin)bin;
                        if (numericBin.belongsToBin(feature)) {
                            return 0;
                        } else if (numericBin.isSmaller(feature)) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                });

                if (binPosition < 0) {
                    if (((NumericBin)bins.get(0)).isSmaller(feature)) {
                        ret.add(bins.get(0));
                    } else {
                        ret.add(bins.get(bins.size() - 1));
                    }
                } else {
                    ret.add(bins.get(binPosition));
                }
            }
            return ret;
        }
    }

    private static final class NumericBin extends NumericFeature {
        private final Double lowerBound;
        private final Double upperBound;

        /**
         * <p>
         * 
         * </p>
         * 
         * @param name
         * @param value
         */
        public NumericBin(String name, Double lowerBound, Double upperBound, Double index) {
            super(name, index);
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        public boolean belongsToBin(NumericFeature feature) {
            return lowerBound <= feature.getValue() && upperBound > feature.getValue();
        }

        public boolean isSmaller(NumericFeature feature) {
            return feature.getValue() < lowerBound;
        }

        public boolean isLarger(NumericFeature feature) {
            return feature.getValue() >= upperBound;
        }

    }

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
     * @param featurePath The feature name if you have a flat {@link FeatureVector} or the featurePath otherwise.
     * @param dataset The collection of instances to select features for.
     * @return
     */
    public static <T extends Feature<?>> Map<T, Double> calculateInformationGain(String featurePath,
            Class<T> featureType, Collection<Instance> dataset) {
        Map<T, Double> ret = CollectionHelper.newHashMap();
        Map<String, Integer> classCounts = CollectionHelper.newHashMap();
        Map<String, Integer> featuresInClass = CollectionHelper.newHashMap();
        Integer sumOfFeaturesInAllClasses = 0;
        Map<T, Integer> absoluteOccurences = CollectionHelper.newHashMap();
        Map<T, Map<String, Integer>> absoluteConditionalOccurences = CollectionHelper.newHashMap();

        // initialize binner if feature is numeric.
        Binner binner = null;
        if (featureType.equals(NumericFeature.class)) {
            binner = discretize(featurePath, dataset);
        }

        for (Instance instance : dataset) {
            Integer targetClassCount = classCounts.get(instance.getTargetClass());
            if (targetClassCount == null) {
                classCounts.put(instance.getTargetClass(), 1);
            } else {
                classCounts.put(instance.getTargetClass(), ++targetClassCount);
            }

            // get all features of the current feature type from the feature vector.
            List<T> featuresList = FeatureUtils
                    .getFeaturesAtPath(instance.getFeatureVector(), featureType, featurePath);
            // This is necessary to handle numeric features.
            @SuppressWarnings("unchecked")
            List<T> binnedFeatureList = binner == null ? featuresList : (List<T>)binner
                    .bin((List<NumericFeature>)featuresList);
            Set<T> features = new HashSet<T>(binnedFeatureList); // remove duplicates

            // count feature occurrences
            for (T feature : features) {
                // refresh the counter how often the feature occurs together with the class of the current instance.
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
                // int classCount = classCounts.get(absoluteConditionalOccurence.getKey());
                // Probability for class ci containing term t
                double Prcit = laplaceSmooth(absoluteOccurences.keySet().size(),
                        featuresInClass.get(absoluteConditionalOccurence.getKey()),
                        absoluteConditionalOccurence.getValue());
                termClassCoocurrence += Prcit * Math.log(Prcit);

                // Probability for class ci not containing term t
                double Prcint = laplaceSmooth(absoluteOccurences.keySet().size(), sumOfFeaturesInAllClasses
                        - featuresInClass.get(absoluteConditionalOccurence.getKey()), dataset.size()
                        - absoluteConditionalOccurence.getValue());
                termClassNonCoocurrence = Prcint * Math.log(Prcint);
            }
            double termProb = absoluteOccurence.getValue().doubleValue() / dataset.size() + termClassCoocurrence;

            double nonTermProb = (double)(dataset.size() - absoluteOccurence.getValue()) / dataset.size()
                    + termClassNonCoocurrence;

            G = -classProb + termProb + nonTermProb;
            ret.put(absoluteOccurence.getKey(), G);
        }
        return ret;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param featurePath
     * @param dataset
     * @return
     */
    private static Binner discretize(final String featurePath, Collection<Instance> dataset) {
        List<Instance> sortedDataset = new LinkedList<Instance>(dataset);
        Collections.sort(sortedDataset, new Comparator<Instance>() {

            @Override
            public int compare(Instance o1, Instance o2) {
                List<NumericFeature> o1Features = FeatureUtils.getFeaturesAtPath(o1.getFeatureVector(),
                        NumericFeature.class, featurePath);
                List<NumericFeature> o2Features = FeatureUtils.getFeaturesAtPath(o2.getFeatureVector(),
                        NumericFeature.class, featurePath);
                Validate.isTrue(o1Features.size() == 1 && o2Features.size() == 1,
                        "Feature %s is either sparse or not available", featurePath);
                return o1Features.get(0).getValue().compareTo(o2Features.get(0).getValue());
            }
        });

        List<Integer> boundaryPoints = findBoundaryPoints(sortedDataset, featurePath);
        String name = featurePath.split("/")[0];
        List<NumericFeature> features = new ArrayList<NumericFeature>();
        for (Instance instance : sortedDataset) {
            List<NumericFeature> feature = FeatureUtils.getFeaturesAtPath(instance.getFeatureVector(),
                    NumericFeature.class, featurePath);
            Validate.isTrue(feature.size() == 1);
            features.addAll(feature);
        }
        return new Binner(name, boundaryPoints, features);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param sortedDataset
     * @return
     */
    private static List<Integer> findBoundaryPoints(List<Instance> sortedDataset, String featurePath) {
        List<Integer> boundaryPoints = new ArrayList<Integer>();
        int N = sortedDataset.size();
        List<Instance> s = sortedDataset;
        String a = featurePath;
        for (int t = 1; t < sortedDataset.size(); t++) {
            if (sortedDataset.get(t - 1).getTargetClass() != sortedDataset.get(t).getTargetClass()
                    && gain(t, s) > (Math.log(N - 1) / Math.log(2)) - N + delta(t, s) / N) {
                boundaryPoints.add(t);
            }
        }
        return boundaryPoints;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param t
     * @param s
     * @return
     */
    private static double gain(int t, List<Instance> s) {
        List<Instance> s1 = s.subList(0, t);
        List<Instance> s2 = s.subList(t, s.size());
        return entropy(s) - (s1.size() * entropy(s1)) / s.size() - (s2.size() * entropy(s2)) / s.size();
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param t
     * @param s
     * @return
     */
    private static double delta(int t, List<Instance> s) {
        double k = calculateNumberOfClasses(s);
        List<Instance> s1 = s.subList(0, t);
        List<Instance> s2 = s.subList(t, s.size());
        double k1 = calculateNumberOfClasses(s1);
        double k2 = calculateNumberOfClasses(s2);
        return Math.log(Math.pow(3.0d, k) - 2) / Math.log(2.0d)
                - (k * entropy(s) - k1 * entropy(s1) - k2 * entropy(s2));
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param s
     * @return
     */
    private static double calculateNumberOfClasses(List<Instance> s) {
        Set<String> possibleClasses = new HashSet<String>();
        for (Instance instance : s) {
            possibleClasses.add(instance.getTargetClass());
        }
        return possibleClasses.size();
    }

    private static double entropy(List<Instance> dataset) {
        double entropy = 0.0d;
        Map<String, Integer> absoluteOccurrences = new HashMap<String, Integer>();
        Set<String> targetClasses = new HashSet<String>();
        for (Instance instance : dataset) {
            targetClasses.add(instance.getTargetClass());
            Integer absoluteCount = absoluteOccurrences.get(instance.getTargetClass());
            if (absoluteCount == null) {
                absoluteCount = 0;
            }
            absoluteOccurrences.put(instance.getTargetClass(), ++absoluteCount);
        }
        for (String targetClass : targetClasses) {
            double probability = (double)absoluteOccurrences.get(targetClass) / dataset.size();
            entropy -= probability * Math.log(probability) / Math.log(2);
        }
        return entropy;
    }

    private static double laplaceSmooth(int vocabularySize, int countOfFeature, int countOfCoocurence) {
        return (1.0d + countOfCoocurence) / (vocabularySize + countOfFeature);
    }
}

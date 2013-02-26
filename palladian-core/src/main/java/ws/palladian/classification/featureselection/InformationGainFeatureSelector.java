/**
 * Created on: 05.02.2013 15:55:54
 */
package ws.palladian.classification.featureselection;

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

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureUtils;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A {@link FeatureSelector} applying the information gain selection criterion.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class InformationGainFeatureSelector implements FeatureSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformationGainFeatureSelector.class);

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
    public <T extends Feature<?>> Map<T, Double> calculateInformationGain(String featurePath, Class<T> featureType,
            Collection<Instance> dataset) {
        Validate.notNull(featurePath);
        Validate.notNull(featureType);
        Validate.notNull(dataset);
        if (dataset.isEmpty()) {
            LOGGER.warn("Dataset for feature selection is empty. No feature selection is carried out.");
        }

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

        // count occurences in dataset
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

        // calculate information gain.
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
     * Descretize {@link NumericFeature}s following the algorithm proposed by Fayyad and Irani in
     * "Multi-Interval Discretization of Continuous-Valued Attributes for Classification Learning."
     * </p>
     * 
     * @param featurePath The path to the {@link NumericFeature} to discretize.
     * @param dataset The dataset to base the discretization on. The provided {@link Instance}s should contain the
     *            {@link Feature} for this algorithm to work.
     * @return A {@link Binner} object capable of discretization of already encountered and unencountered values for the
     *         provided {@link NumericFeature}.
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
                try {
                    Validate.isTrue(o1Features.size() == 1 && o2Features.size() == 1,
                            "Feature %s is either sparse or not available", featurePath);
                } catch (Exception e) {
                    System.out.println("###############");
                }
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

    private static double gain(int t, List<Instance> s) {
        List<Instance> s1 = s.subList(0, t);
        List<Instance> s2 = s.subList(t, s.size());
        return entropy(s) - (s1.size() * entropy(s1)) / s.size() - (s2.size() * entropy(s2)) / s.size();
    }

    private static double delta(int t, List<Instance> s) {
        double k = calculateNumberOfClasses(s);
        List<Instance> s1 = s.subList(0, t);
        List<Instance> s2 = s.subList(t, s.size());
        double k1 = calculateNumberOfClasses(s1);
        double k2 = calculateNumberOfClasses(s2);
        return Math.log(Math.pow(3.0d, k) - 2) / Math.log(2.0d)
                - (k * entropy(s) - k1 * entropy(s1) - k2 * entropy(s2));
    }

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

    @Override
    public FeatureRanking rankFeatures(Collection<Instance> dataset, Collection<FeatureDetails> featuresToConsider) {
        FeatureRanking ranking = new FeatureRanking();

        for (FeatureDetails featureDetails : featuresToConsider) {
            Map<? extends Feature<?>, Double> informationGainValues = calculateInformationGain(
                    featureDetails.getFeaturePath(), featureDetails.getFeatureType(), dataset);

            if (featureDetails.isSparse()) {
                // add each entry
                for (Entry<? extends Feature<?>, Double> value : informationGainValues.entrySet()) {
                    ranking.addSparse(value.getKey().getName(), value.getKey().getValue().toString(), value.getValue());
                }
            } else {
                // calc average
                double averageGain = 0.0;
                for (Entry<? extends Feature<?>, Double> value : informationGainValues.entrySet()) {
                    averageGain += value.getValue();
                }
                averageGain /= informationGainValues.size();
                ranking.add(featureDetails.getFeaturePath(), averageGain);
            }
        }
        return ranking;
    }
}

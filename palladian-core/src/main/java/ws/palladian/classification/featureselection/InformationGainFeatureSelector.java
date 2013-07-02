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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Instance;
import ws.palladian.classification.discretization.Binner;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.utils.FeatureDescriptor;
import ws.palladian.processing.features.utils.FeatureUtils;

/**
 * <p>
 * A {@link FeatureRanker} applying the information gain selection criterion.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.2.0
 */
public final class InformationGainFeatureSelector extends AbstractFeatureRanker {

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
    private Map<Feature<?>, Double> calculateInformationGain(final Collection<Instance> dataset) {
        Validate.notNull(dataset);
        Map<Feature<?>, Double> ret = CollectionHelper.newHashMap();
        if (dataset.isEmpty()) {
            LOGGER.warn("Dataset for feature selection is empty. No feature selection is carried out.");
            return ret;
        }
        
        Collection<Pair<Set<Feature<?>>,String>> preparedData = prepare(dataset);
        Map<String, Double> classPriors = calculateTargetClassPriors(dataset);
     // All occurrences of feature in the dataset
        Map<Feature<?>, Integer> absoluteOccurences = CollectionHelper.newHashMap();
     // Occurrences of feature together with a certain target class.
        Map<Feature<?>, Map<String, Integer>> absoluteConditionalOccurences = CollectionHelper.newHashMap();
        // the following two lines: vocabulary size required for smoothing
        Map<String, Integer> featuresInClass = CollectionHelper.newHashMap();
        Integer sumOfFeaturesInAllClasses = 0;
        
        for(Pair<Set<Feature<?>>, String> instance : preparedData) {
            
            for(Feature<?> feature:instance.getLeft()) {
                
                // count absolute occurrences
                Integer absoluteOccurrencesOfFeature = absoluteOccurences.get(feature);
                if(absoluteOccurrencesOfFeature==null) {
                    absoluteOccurrencesOfFeature = 0;
                }
                absoluteOccurrencesOfFeature++;
                absoluteOccurences.put(feature, absoluteOccurrencesOfFeature);
                
                // count conditional occurrences with all classes
                Map<String, Integer> absoluteConditionalOccurence = absoluteConditionalOccurences.get(feature);
                if (absoluteConditionalOccurence == null) {
                    absoluteConditionalOccurence = CollectionHelper.newHashMap();
                }
                Integer occurrenceInTargetClass = absoluteConditionalOccurence.get(instance.getRight());
                if(occurrenceInTargetClass==null) {
                    occurrenceInTargetClass = 0;
                }
                occurrenceInTargetClass++;
                absoluteConditionalOccurence.put(instance.getRight(), occurrenceInTargetClass);
                absoluteConditionalOccurences.put(feature, absoluteConditionalOccurence);
                
                // calculate the vocabulary size for smoothing.
                Integer countOfFeaturesInClass = featuresInClass.get(instance.getTargetClass());
                if (countOfFeaturesInClass == null) {
                    countOfFeaturesInClass = 0;
                }
                featuresInClass.put(instance.getRight(), ++countOfFeaturesInClass);
                sumOfFeaturesInAllClasses++;
            }
        }
        
        // calculate dataset constant class probability (first summand)
        double classProb = 0.0d;
        for (Entry<String, Double> classCount : classPriors.entrySet()) {
            double Prci = classCount.getValue();
            classProb += Prci * Math.log(Prci);
        }

        // calculate information gain.
        for (Entry<Feature<?>, Integer> absoluteOccurence : absoluteOccurences.entrySet()) {
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
                termClassNonCoocurrence += Prcint * Math.log(Prcint);
            }
            double termProb = absoluteOccurence.getValue().doubleValue() / dataset.size() * termClassCoocurrence;

            double nonTermProb = (double)(dataset.size() - absoluteOccurence.getValue()) / dataset.size()
                    * termClassNonCoocurrence;

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
     * @param dataset
     * @return
     */
    private Collection<Pair<FeatureVector, String>> prepare(Collection<Instance> dataset) {
        // discretize numeric features
        Set<T> features = convertToSet(instance.getLeft());
        return null;
    }

    /**
     * <p>
     * 
     * </p>
     *
     * @param dataset The dataset to calculate the target class priors for.
     * @return A mapping from target class to prior.
     */
    private Map<String, Double> calculateTargetClassPriors(final Collection<Instance> dataset) {
        Map<String, Double> ret = CollectionHelper.newHashMap();
        Map<String, Integer> absoluteOccurrences = CollectionHelper.newHashMap();
        
        for(Instance instance:dataset) {
            Integer absoluteOccurrenceOfClass = absoluteOccurrences.get(instance.getTargetClass());
            if(absoluteOccurrenceOfClass==null) {
                absoluteOccurrenceOfClass = 0;
            }
            absoluteOccurrenceOfClass++;
            absoluteOccurrences.put(instance.getTargetClass(),absoluteOccurrenceOfClass);
        }
        
        for(Entry<String, Integer> absoluteOccurrenceOfClass:absoluteOccurrences.entrySet()) {
            ret.put(absoluteOccurrenceOfClass.getKey(), absoluteOccurrenceOfClass.getValue().doubleValue()/dataset.size());
        }
        
        return ret;
    }

//    /**
//     * <p>
//     * Descretize {@link NumericFeature}s following the algorithm proposed by Fayyad and Irani in
//     * "Multi-Interval Discretization of Continuous-Valued Attributes for Classification Learning."
//     * </p>
//     * 
//     * @param featurePath The path to the {@link NumericFeature} to discretize.
//     * @param dataset The dataset to base the discretization on. The provided {@link Instance}s should contain the
//     *            {@link Feature} for this algorithm to work.
//     * @return A {@link Binner} object capable of discretization of already encountered and unencountered values for the
//     *         provided {@link NumericFeature}.
//     */
//    public static Map<List<FeatureDescriptor>, Binner> discretize(final FeatureDetails featureDetails,
//            Collection<Instance> dataset) {
//        Map<List<FeatureDescriptor>, List<Instance>> sortedInstances = new HashMap<List<FeatureDescriptor>, List<Instance>>();
//        for (Instance instance : dataset) {
//            List<Pair<List<FeatureDescriptor>, Feature<?>>> features = FeatureUtils.getIdentifiedFeaturesAtPath(
//                    instance.getFeatureVector(), featureDetails.getPath(), new ArrayList<FeatureDescriptor>());
//            for (Pair<List<FeatureDescriptor>, Feature<?>> feature : features) {
//                final List<FeatureDescriptor> featureIdentifier = feature.getKey();
//
//                List<Instance> sorted = sortedInstances.get(featureIdentifier);
//                if (sorted == null) {
//                    sorted = new ArrayList<Instance>();
//                }
//                sorted.add(instance);
//                Collections.sort(sorted, new Comparator<Instance>() {
//
//                    @Override
//                    public int compare(Instance o1, Instance o2) {
//                        NumericFeature o1Feature = FeatureUtils.getFeatureForIdentifier(o1.getFeatureVector(),
//                                NumericFeature.class, featureIdentifier);
//                        NumericFeature o2Feature = FeatureUtils.getFeatureForIdentifier(o2.getFeatureVector(),
//                                NumericFeature.class, featureIdentifier);
//                        return o1Feature.getValue().compareTo(o2Feature.getValue());
//                    }
//                });
//                sortedInstances.put(featureIdentifier, sorted);
//            }
//        }
//
//        Map<List<FeatureDescriptor>, Binner> ret = new HashMap<List<FeatureDescriptor>, Binner>();
//        for (Entry<List<FeatureDescriptor>, List<Instance>> entry : sortedInstances.entrySet()) {
//            Binner binner = createBinner(entry.getValue(), entry.getKey());
//            ret.put(entry.getKey(), binner);
//        }
//        return ret;
//    }

//    /**
//     * <p>
//     * Creates a new {@link Binner} for the {@link NumericFeature} identified by the provided {@code featureName}.
//     * </p>
//     *
//     * @param dataset the dataset containing the provided feature.
//     * @param featureName
//     * @return
//     */
//    private static Binner createBinner(List<Instance> dataset, final String featureName) {
//        List<Integer> boundaryPoints = findBoundaryPoints(dataset);
//
//        StringBuilder nameBuilder = new StringBuilder();
////        for (FeatureDescriptor descriptor : featureName) {
////            nameBuilder.append(descriptor.toString()).append("/");
////        }
//
//        List<NumericFeature> features = new ArrayList<NumericFeature>();
//        for (Instance instance : dataset) {
//            NumericFeature feature = instance.getFeatureVector().get(NumericFeature.class, featureName);
//            features.add(feature);
//        }
//        return new Binner(nameBuilder.toString(), boundaryPoints, features);
//    }

//    private static List<Integer> findBoundaryPoints(List<Instance> sortedDataset) {
//        List<Integer> boundaryPoints = new ArrayList<Integer>();
//        int N = sortedDataset.size();
//        List<Instance> s = sortedDataset;
//        for (int t = 1; t < sortedDataset.size(); t++) {
//            if (sortedDataset.get(t - 1).getTargetClass() != sortedDataset.get(t).getTargetClass()
//                    && gain(t, s) > (Math.log(N - 1) / Math.log(2)) - N + delta(t, s) / N) {
//                boundaryPoints.add(t);
//            }
//        }
//        return boundaryPoints;
//    }

//    private static double gain(int t, List<Instance> s) {
//        List<Instance> s1 = s.subList(0, t);
//        List<Instance> s2 = s.subList(t, s.size());
//        return entropy(s) - (s1.size() * entropy(s1)) / s.size() - (s2.size() * entropy(s2)) / s.size();
//    }
//
//    private static double delta(int t, List<Instance> s) {
//        double k = calculateNumberOfClasses(s);
//        List<Instance> s1 = s.subList(0, t);
//        List<Instance> s2 = s.subList(t, s.size());
//        double k1 = calculateNumberOfClasses(s1);
//        double k2 = calculateNumberOfClasses(s2);
//        return Math.log(Math.pow(3.0d, k) - 2) / Math.log(2.0d)
//                - (k * entropy(s) - k1 * entropy(s1) - k2 * entropy(s2));
//    }

//    private static double entropy(List<Instance> dataset) {
//        double entropy = 0.0d;
//        Map<String, Integer> absoluteOccurrences = new HashMap<String, Integer>();
//        Set<String> targetClasses = new HashSet<String>();
//        for (Instance instance : dataset) {
//            targetClasses.add(instance.getTargetClass());
//            Integer absoluteCount = absoluteOccurrences.get(instance.getTargetClass());
//            if (absoluteCount == null) {
//                absoluteCount = 0;
//            }
//            absoluteOccurrences.put(instance.getTargetClass(), ++absoluteCount);
//        }
//        for (String targetClass : targetClasses) {
//            double probability = (double)absoluteOccurrences.get(targetClass) / dataset.size();
//            entropy -= probability * Math.log(probability) / Math.log(2);
//        }
//        return entropy;
//    }

//    private static double calculateNumberOfClasses(List<Instance> s) {
//        Set<String> possibleClasses = new HashSet<String>();
//        for (Instance instance : s) {
//            possibleClasses.add(instance.getTargetClass());
//        }
//        return possibleClasses.size();
//    }

    private static double laplaceSmooth(int vocabularySize, int countOfFeature, int countOfCoocurence) {
        return (1.0d + countOfCoocurence) / (vocabularySize + countOfFeature);
    }

    @Override
    public FeatureRanking rankFeatures(Collection<Instance> dataset, Collection<FeatureDetails> featuresToConsider) {
        FeatureRanking ranking = new FeatureRanking();

        List<String> featureNames = getDistinctFeatureNames(dataset);
        for (String name : featureNames) {
            Map<? extends Feature<?>, Double> informationGainValues = calculateInformationGain(name, dataset);

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
                ranking.add(name, averageGain);
            }
        }
        return ranking;
    }
}

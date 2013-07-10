/**
 * Created on 30.06.2013 12:28:52
 */
package ws.palladian.classification.featureselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Instance;
import ws.palladian.classification.discretization.Binner;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Abstract base class for all {@link FeatureRanker}s. Implements common base functionality.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
public abstract class AbstractFeatureRanker implements FeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureRanker.class);

    private final Map<String, Binner> binnerCache = CollectionHelper.newHashMap();

    /**
     * <p>
     * Converts all features within a {@link FeatureVector} to a {@link Set}. For dense {@link Feature}s the set is
     * extended by only one element. For sparse {@link Feature}s it contains all instances from the
     * {@link FeatureVector} exactly once. This means, for example, if the term 'the' occurs two times in a text, this
     * method will only include it once in the returned {@link Set}.
     * </p>
     * <p>
     * The method is used for deduplication of features.
     * </p>
     * 
     * @param featureVector The {@link FeatureVector} containing the dense {@link Feature} or sparse {@link Feature}s
     * @return the {@link Feature} or {@link Feature}s as a {@link Set}.
     */
    @SuppressWarnings("unchecked")
    protected Set<Feature<?>> convertToSet(FeatureVector featureVector, Collection<? extends Trainable> dataset) {
        Set<Feature<?>> ret = CollectionHelper.newHashSet();
        boolean firstRun = false;
        if (binnerCache.isEmpty()) {
            LOGGER.info("Converting {} features to set", featureVector.size());
            firstRun = true;
        }
        StopWatch stopWatch = new StopWatch();
        int counter = 0;

        for (final Feature<?> feature : featureVector) {
            if (feature instanceof ListFeature<?>) {
                ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)feature;
                for (final Feature<?> element : listFeature) {
                    if (element instanceof NumericFeature) {
                        Binner binner = binnerCache.get(element.getName());
                        if (binner == null) {
                            LOGGER.info(ProgressHelper.getProgress(counter++, featureVector.size(), 1, stopWatch));
                            binner = discretize(element.getName(), dataset, new Comparator<Trainable>() {

                                @Override
                                public int compare(Trainable i1, Trainable i2) {
                                    ListFeature<NumericFeature> i1ListFeature = i1.getFeatureVector().get(
                                            ListFeature.class, feature.getName());
                                    ListFeature<NumericFeature> i2ListFeature = i2.getFeatureVector().get(
                                            ListFeature.class, feature.getName());

                                    NumericFeature i1Feature = i1ListFeature.getFeatureWithName(element.getName());
                                    NumericFeature i2Feature = i2ListFeature.getFeatureWithName(element.getName());
                                    Double i1FeatureValue = i1Feature == null ? Double.MIN_VALUE : i1Feature.getValue();
                                    Double i2FeatureValue = i2Feature == null ? Double.MIN_VALUE : i2Feature.getValue();
                                    return i1FeatureValue.compareTo(i2FeatureValue);
                                }
                            });
                            binnerCache.put(element.getName(), binner);
                        }
                        ret.add(binner.bin((NumericFeature)element));
                    } else {
                        ret.add(element);
                    }
                }
            } else if (feature instanceof NumericFeature) {
                Binner binner = binnerCache.get(feature.getName());
                if (binner == null) {
                    LOGGER.info(ProgressHelper.getProgress(counter++, featureVector.size(), 1, stopWatch));
                    binner = discretize(feature.getName(), dataset, new Comparator<Trainable>() {

                        @Override
                        public int compare(Trainable i1, Trainable i2) {
                            NumericFeature i1Feature = i1.getFeatureVector().get(NumericFeature.class,
                                    feature.getName());
                            NumericFeature i2Feature = i2.getFeatureVector().get(NumericFeature.class,
                                    feature.getName());
                            Double i1FeatureValue = i1Feature == null ? Double.MIN_VALUE : i1Feature.getValue();
                            Double i2FeatureValue = i2Feature == null ? Double.MIN_VALUE : i2Feature.getValue();
                            return i1FeatureValue.compareTo(i2FeatureValue);
                        }
                    });
                    binnerCache.put(feature.getName(), binner);
                }
                ret.add(binner.bin((NumericFeature)feature));
            } else {
                ret.add(feature);
                counter++;
            }
        }
        if (firstRun) {
            LOGGER.info("Finished converting in {}", stopWatch);
        }
        return ret;
    }

    /**
     * <p>
     * Descretize {@link NumericFeature}s following the algorithm proposed by Fayyad and Irani in
     * "Multi-Interval Discretization of Continuous-Valued Attributes for Classification Learning."
     * </p>
     * 
     * @param featureName The path to the {@link NumericFeature} to discretize.
     * @param dataset The dataset to base the discretization on. The provided {@link Instance}s should contain the
     *            {@link Feature} for this algorithm to work.
     * @return A {@link Binner} object capable of discretization of already encountered and unencountered values for the
     *         provided {@link NumericFeature}.
     */
    public static Binner discretize(final String featureName, Collection<? extends Trainable> dataset,
            Comparator<Trainable> comparator) {
//        List<Trainable> sortedInstances = CollectionHelper.newArrayList();
//        for (Trainable instance : dataset) {
//
//            sortedInstances.add(instance);
//            Collections.sort(sortedInstances, comparator);
//        }
        List<Trainable> sortedInstances = new ArrayList<Trainable>(dataset);
        Collections.sort(sortedInstances, comparator);
        return createBinner(sortedInstances, featureName);
    }

    /**
     * <p>
     * Creates a new {@link Binner} for the {@link NumericFeature} identified by the provided {@code featureName}.
     * </p>
     * 
     * @param dataset the dataset containing the provided feature.
     * @param featureName
     * @return
     */
    private static Binner createBinner(List<Trainable> dataset, String featureName) {
        List<Integer> boundaryPoints = findBoundaryPoints(dataset);

        // StringBuilder nameBuilder = new StringBuilder();
        // for (FeatureDescriptor descriptor : featureName) {
        // nameBuilder.append(descriptor.toString()).append("/");
        // }

        List<NumericFeature> features = new ArrayList<NumericFeature>();
        for (Trainable instance : dataset) {
            NumericFeature feature = instance.getFeatureVector().get(NumericFeature.class, featureName);
            features.add(feature);
        }
        return new Binner(featureName, boundaryPoints, features);
    }

    /**
     * <p>
     * Find all the boundary points within the provided dataset. The dataset needs to be sorted based on the
     * {@link NumericFeature} the boundary points are searched for.
     * </p>
     * 
     * @param sortedDataset The dataset sorted by the values of the searched {@link NumericFeature}.
     * @return The boundary points as a set of indices into the sorted dataset. Each element marks the location of one
     *         boundary point. Each boundary point as such lies between the index provided by an element of the return
     *         value and the previous index. So a value of 1 means there is a boundary point between the 0th and the 1st
     *         instance.
     */
    private static List<Integer> findBoundaryPoints(List<Trainable> sortedDataset) {
        List<Integer> boundaryPoints = new ArrayList<Integer>();
        int N = sortedDataset.size();
        List<Trainable> s = sortedDataset;
        for (int t = 1; t < sortedDataset.size(); t++) {
//            if (!sortedDataset.get(t - 1).getTargetClass().equals(sortedDataset.get(t).getTargetClass())
            if (sortedDataset.get(t - 1).getTargetClass() != sortedDataset.get(t).getTargetClass()
                    && gain(t, s) > (Math.log(N - 1) / Math.log(2)) - N + delta(t, s) / N) {
                boundaryPoints.add(t);
            }
        }
        return boundaryPoints;
    }

    private static double gain(int t, List<Trainable> s) {
        List<Trainable> s1 = s.subList(0, t);
        List<Trainable> s2 = s.subList(t, s.size());
        return entropy(s) - (s1.size() * entropy(s1)) / s.size() - (s2.size() * entropy(s2)) / s.size();
    }

    private static double delta(int t, List<Trainable> s) {
        double k = calculateNumberOfClasses(s);
        List<Trainable> s1 = s.subList(0, t);
        List<Trainable> s2 = s.subList(t, s.size());
        double k1 = calculateNumberOfClasses(s1);
        double k2 = calculateNumberOfClasses(s2);
        return Math.log(Math.pow(3.0d, k) - 2) / Math.log(2.0d)
                - (k * entropy(s) - k1 * entropy(s1) - k2 * entropy(s2));
    }

    private static double entropy(List<Trainable> dataset) {
        double entropy = 0.0d;
        // Map<String, Integer> absoluteOccurrences = new HashMap<String, Integer>();
        // Set<String> targetClasses = new HashSet<String>();
        CountMap<String> occurrences = CountMap.create();
        for (Trainable instance : dataset) {
//            targetClasses.add(instance.getTargetClass());
//            Integer absoluteCount = absoluteOccurrences.get(instance.getTargetClass());
//            if (absoluteCount == null) {
//                absoluteCount = 0;
//            }
//            absoluteOccurrences.put(instance.getTargetClass(), ++absoluteCount);
            occurrences.add(instance.getTargetClass());
        }
//        for (String targetClass : targetClasses) {
//            double probability = (double)absoluteOccurrences.get(targetClass) / dataset.size();
//            entropy -= probability * Math.log(probability) / Math.log(2);
//        }
        for (String targetClass : occurrences.uniqueItems()) {
            double probability = (double)occurrences.getCount(targetClass) / dataset.size();
            entropy -= probability * Math.log(probability) / Math.log(2);
        }
        return entropy;
    }

    /**
     * <p>
     * Calculates the number of target classes from a list of {@link Instance}s.
     * </p>
     * 
     * @param s The set of instances to calculate the target classes for.
     * @return the number of target classes the provided dataset contains.
     */
    private static double calculateNumberOfClasses(List<Trainable> s) {
        Set<String> possibleClasses = new HashSet<String>();
        for (Trainable instance : s) {
            possibleClasses.add(instance.getTargetClass());
        }
        return possibleClasses.size();
    }

}

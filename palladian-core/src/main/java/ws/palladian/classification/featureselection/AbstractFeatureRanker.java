/**
 * Created on 30.06.2013 12:28:52
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
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.classification.Instance;
import ws.palladian.classification.discretization.Binner;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.utils.FeatureDescriptor;
import ws.palladian.processing.features.utils.FeatureUtils;

/**
 * <p>
 * Abstract base class for all {@link FeatureRanker}s. Implements common base functionallity.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
public abstract class AbstractFeatureRanker implements FeatureRanker {
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
    protected static Set<Feature<?>> convertToSet(final FeatureVector featureVector, final Collection<Instance> dataset) {
        Set<Feature<?>> ret = CollectionHelper.newHashSet();
        Map<String, Binner> binners = CollectionHelper.newHashMap();

        for (final Feature<?> feature : featureVector) {
            if (feature instanceof ListFeature<?>) {
                ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)feature;
                for (final Feature<?> element : listFeature) {
                    if (element instanceof NumericFeature) {
                        Binner binner = binners.get(element.getName());
                        if (binner == null) {
                            binner = discretize(element.getName(), dataset, new Comparator<Instance>() {

                                @Override
                                public int compare(Instance i1, Instance i2) {
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
                            binners.put(element.getName(), binner);
                        }
                        ret.add(binner.bin((NumericFeature)element));
                    } else {
                        ret.add(element);
                    }
                }
            } else if (feature instanceof NumericFeature) {
                Binner binner = binners.get(feature.getName());
                if (binner == null) {
                    binner = discretize(feature.getName(), dataset, new Comparator<Instance>() {

                        @Override
                        public int compare(Instance i1, Instance i2) {
                            NumericFeature i1Feature = i1.getFeatureVector().get(NumericFeature.class,
                                    feature.getName());
                            NumericFeature i2Feature = i2.getFeatureVector().get(NumericFeature.class,
                                    feature.getName());
                            Double i1FeatureValue = i1Feature == null ? Double.MIN_VALUE : i1Feature.getValue();
                            Double i2FeatureValue = i2Feature == null ? Double.MIN_VALUE : i2Feature.getValue();
                            return i1FeatureValue.compareTo(i2FeatureValue);
                        }
                    });
                    binners.put(feature.getName(), binner);
                }

                ret.add(binner.bin((NumericFeature)feature));
            } else {
                ret.add(feature);
            }
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
    public static Binner discretize(final String featureName, Collection<Instance> dataset,
            Comparator<Instance> comparator) {
        List<Instance> sortedInstances = CollectionHelper.newArrayList();
        for (Instance instance : dataset) {

            sortedInstances.add(instance);
            Collections.sort(sortedInstances, comparator);
        }

        Binner binner = createBinner(sortedInstances, featureName);
        return binner;
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
    private static Binner createBinner(List<Instance> dataset, final String featureName) {
        List<Integer> boundaryPoints = findBoundaryPoints(dataset);

//        StringBuilder nameBuilder = new StringBuilder();
        // for (FeatureDescriptor descriptor : featureName) {
        // nameBuilder.append(descriptor.toString()).append("/");
        // }

        List<NumericFeature> features = new ArrayList<NumericFeature>();
        for (Instance instance : dataset) {
            NumericFeature feature = instance.getFeatureVector().get(NumericFeature.class, featureName);
            features.add(feature);
        }
        return new Binner(featureName, boundaryPoints, features);
    }

    private static List<Integer> findBoundaryPoints(List<Instance> sortedDataset) {
        List<Integer> boundaryPoints = new ArrayList<Integer>();
        int N = sortedDataset.size();
        List<Instance> s = sortedDataset;
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

    private static double calculateNumberOfClasses(List<Instance> s) {
        Set<String> possibleClasses = new HashSet<String>();
        for (Instance instance : s) {
            possibleClasses.add(instance.getTargetClass());
        }
        return possibleClasses.size();
    }

}

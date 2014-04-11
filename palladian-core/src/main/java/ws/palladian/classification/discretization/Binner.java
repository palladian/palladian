package ws.palladian.classification.discretization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.ImmutableDoubleValue;
import ws.palladian.core.Instance;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class Binner {

    /**
     * <p>
     * Creates a new {@link Binner} for the {@link NumericFeature} identified by the provided {@code featureName}.
     * </p>
     * 
     * @param dataset the dataset containing the provided feature.
     * @param featureName
     * @return
     */
    public static Binner createBinner(List<Instance> dataset, String featureName) {
        CollectionHelper.print(dataset);

        List<Integer> boundaryPoints = findBoundaryPoints(dataset);
        List<NumericValue> features = CollectionHelper.newArrayList();
        for (Instance instance : dataset) {
            Value value = instance.getVector().get(featureName);
            if (value != null) {
                features.add((NumericValue)value);
            }else{
                features.add(new ImmutableDoubleValue(0));
            }
        }
        return new Binner(featureName, boundaryPoints, features);
    }

    private final List<NumericBin> bins;

    public Binner(String binName, List<Integer> boundaryPoints, List<NumericValue> features) {
        System.out.println("binName="+binName);
        System.out.println("boundaryPoints="+boundaryPoints);
        System.out.println("features="+features);
        
        Validate.notEmpty(features);
        bins = CollectionHelper.newArrayList();

        // boundary points may be empty if the dataset contains only instances with the same type or only one instance.
        // In this case only one bin for all features is necessary.
        if (boundaryPoints.isEmpty()) {
            double lowerBound = Math.min(features.get(0).getDouble(), 0.0);
            double upperBound = Math.max(features.get(features.size() - 1).getDouble(), 0.0);
            bins.add(new NumericBin(binName, lowerBound, upperBound, 0.0d));
        } else {

            for (int i = 0; i < boundaryPoints.size(); i++) {
                double lowerBound = i == 0 ? features.get(0).getDouble() : features.get(boundaryPoints.get(i - 1))
                        .getDouble();
                double upperBound = features.get(boundaryPoints.get(i)).getDouble();
                bins.add(new NumericBin(binName, lowerBound, upperBound, (double)i));
            }
            bins.add(new NumericBin(binName, features.get(boundaryPoints.get(boundaryPoints.size() - 1)).getDouble(),
                    features.get(features.size() - 1).getDouble(), (double)boundaryPoints.size()));
        }
    }

    public List<NumericBin> bin(List<NumericValue> features) {
        List<NumericBin> ret = new ArrayList<NumericBin>(features.size());
        for (NumericValue feature : features) {
            int binPosition = Collections.binarySearch(bins, feature, new Comparator<NumericValue>() {

                @Override
                public int compare(NumericValue bin, NumericValue feature) {
                    NumericBin numericBin = (NumericBin)bin;
                    if (numericBin.belongsToBin(feature.getDouble())) {
                        return 0;
                    } else if (numericBin.isSmaller(feature.getDouble())) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });

            if (binPosition < 0) {
                if (bins.get(0).isSmaller(feature.getDouble())) {
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

    public NumericBin bin(NumericValue feature) {
        int binPosition = Collections.binarySearch(bins, feature, new Comparator<NumericValue>() {

            @Override
            public int compare(NumericValue bin, NumericValue feature) {
                NumericBin numericBin = (NumericBin)bin;
                if (numericBin.belongsToBin(feature.getDouble())) {
                    return 0;
                } else if (numericBin.isSmaller(feature.getDouble())) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        if (binPosition < 0) {
            if (bins.get(0).isSmaller(feature.getDouble())) {
                return bins.get(0);
            } else {
                return bins.get(bins.size() - 1);
            }
        } else {
            return bins.get(binPosition);
        }
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
    private static List<Integer> findBoundaryPoints(List<Instance> sortedDataset) {
        List<Integer> boundaryPoints = new ArrayList<Integer>();
        int N = sortedDataset.size();
    System.out.println(N);
        List<Instance> s = sortedDataset;
        for (int t = 1; t < sortedDataset.size(); t++) {
            // if (!sortedDataset.get(t - 1).getTargetClass().equals(sortedDataset.get(t).getTargetClass())
            if (sortedDataset.get(t - 1).getCategory() != sortedDataset.get(t).getCategory()
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
        Bag<String> occurrences = Bag.create();
        for (Instance instance : dataset) {
            occurrences.add(instance.getCategory());
        }
        for (String targetClass : occurrences.uniqueItems()) {
            double probability = (double)occurrences.count(targetClass) / dataset.size();
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
    private static double calculateNumberOfClasses(List<Instance> s) {
        Set<String> possibleClasses = new HashSet<String>();
        for (Instance instance : s) {
            possibleClasses.add(instance.getCategory());
        }
        return possibleClasses.size();
    }

}
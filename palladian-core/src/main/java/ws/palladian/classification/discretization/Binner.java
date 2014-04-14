package ws.palladian.classification.discretization;

import static java.lang.Math.pow;
import static ws.palladian.helper.math.MathHelper.log2;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class Binner {

    /**
     * <p>
     * Creates a new {@link Binner} for the {@link NumericFeature} identified by the provided {@code featureName} following the algorithm proposed by Fayyad and Irani in
     * "Multi-Interval Discretization of Continuous-Valued Attributes for Classification Learning."
     * </p>
     * 
     * @param dataset the dataset containing the provided feature.
     * @param featureName The path to the {@link NumericFeature} to discretize.
     * @return A {@link Binner} object capable of discretization of already encountered and unencountered values for the
     *         provided {@link NumericFeature}.
     */
    public static Binner createBinner(Collection<? extends Instance> dataset, final String featureName) {
        Validate.notEmpty(dataset, "dataset must not be empty");
        Validate.notEmpty(featureName, "featureName must not be empty");
        
        List<Instance> sortedInstances = CollectionHelper.newArrayList(dataset);
        Collections.sort(sortedInstances, new Comparator<Instance>() {

            @Override
            public int compare(Instance i1, Instance i2) {
                NumericValue value1 = (NumericValue)i1.getVector().get(featureName);
                NumericValue value2 = (NumericValue)i2.getVector().get(featureName);
                double i1FeatureValue = value1 == null ? Double.MIN_VALUE : value1.getDouble();
                double i2FeatureValue = value2 == null ? Double.MIN_VALUE : value2.getDouble();
                return Double.compare(i1FeatureValue, i2FeatureValue);
            }
        });

        List<Integer> boundaryPoints = findBoundaryPoints(sortedInstances);
        List<Double> values = CollectionHelper.newArrayList();
        for (Instance instance : sortedInstances) {
            Value value = instance.getVector().get(featureName);
            if (value != null) {
                values.add(((NumericValue)value).getDouble());
            }else{
                values.add(0.);
            }
        }
        
        // boundary points may be empty if the dataset contains only instances with the same type or only one instance.
        // In this case only one bin for all features is necessary.
        List<NumericBin> bins = CollectionHelper.newArrayList();
        if (boundaryPoints.isEmpty()) {
            double lowerBound = Math.min(values.get(0), 0.0);
            double upperBound = Math.max(values.get(values.size() - 1), 0.0);
            bins.add(new NumericBin(0, lowerBound, upperBound));
        } else {
            for (int i = 0; i < boundaryPoints.size(); i++) {
                double lowerBound = i == 0 ? values.get(0) : values.get(boundaryPoints.get(i - 1));
                double upperBound = values.get(boundaryPoints.get(i));
                bins.add(new NumericBin(i, lowerBound, upperBound));
            }
            bins.add(new NumericBin(boundaryPoints.size(),
                    values.get(boundaryPoints.get(boundaryPoints.size() - 1)), values.get(values.size() - 1)));
        }
        return new Binner(bins);
    }

    private final List<NumericBin> bins;

    private Binner(List<NumericBin> bins) {
        this.bins = bins;
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
        List<Integer> boundaryPoints = CollectionHelper.newArrayList();
        int N = sortedDataset.size();
        for (int t = 1; t < sortedDataset.size(); t++) {
            if (!sortedDataset.get(t - 1).getCategory().equals(sortedDataset.get(t).getCategory())
                    && gain(t, sortedDataset) > log2(N - 1) - N + delta(t, sortedDataset) / N) {
                boundaryPoints.add(t);
            }
        }
        return boundaryPoints;
    }

    private static double gain(int t, List<Instance> s) {
        List<Instance> s1 = s.subList(0, t);
        List<Instance> s2 = s.subList(t, s.size());
        return entropy(s) - s1.size() * entropy(s1) / s.size() - s2.size() * entropy(s2) / s.size();
    }

    private static double delta(int t, List<Instance> s) {
        int k = new DatasetStatistics(s).getCategoryPriors().size();
        List<Instance> s1 = s.subList(0, t);
        List<Instance> s2 = s.subList(t, s.size());
        int k1 = new DatasetStatistics(s1).getCategoryPriors().size();
        int k2 = new DatasetStatistics(s2).getCategoryPriors().size();
        return log2(pow(3, k) - 2) - (k * entropy(s) - k1 * entropy(s1) - k2 * entropy(s2));
    }

    private static double entropy(List<Instance> dataset) {
        double entropy = 0.0d;
        CategoryEntries priors = new DatasetStatistics(dataset).getCategoryPriors();
        for (Category category : priors) {
            entropy -= category.getProbability() * log2(category.getProbability());
        }
        return entropy;
    }

}
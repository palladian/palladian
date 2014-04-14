package ws.palladian.classification.discretization;

import static java.lang.Math.pow;
import static ws.palladian.helper.math.MathHelper.log2;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class Binner {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Binner.class);

    /**
     * Comparator to sort {@link Instance}s based on a {@link NumericValue}.
     * 
     * @author pk
     */
    private static final class ValueComparator implements Comparator<Instance> {
        private final String featureName;

        private ValueComparator(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public int compare(Instance i1, Instance i2) {
            Value value1 = i1.getVector().get(featureName);
            Value value2 = i2.getVector().get(featureName);
            if (!(value1 instanceof NumericValue)) {
                return !(value2 instanceof NumericValue) ? 0 : -1;
            }
            if (!(value2 instanceof NumericValue)) {
                return 1;
            }
            double double1 = ((NumericValue)value1).getDouble();
            double double2 = ((NumericValue)value2).getDouble();
            return Double.compare(double1, double2);
        }

    }

    private final List<Double> boundaries;

    private final String featureName;

    /**
     * <p>
     * Create a new {@link Binner} for specified numeric feature following the algorithm proposed by Fayyad and Irani in
     * "<a href="http://ijcai.org/Past%20Proceedings/IJCAI-93-VOL2/PDF/022.pdf
     * ">Multi-Interval Discretization of Continuous-Valued Attributes for Classification Learning</a>", 1993.
     * </p>
     * 
     * @param dataset The dataset, not <code>null</code>.
     * @param featureName The name of the numeric feature for which to calculate bins.
     */
    public Binner(Iterable<? extends Instance> dataset, String featureName) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.notEmpty(featureName, "featureName must not be empty");
        this.boundaries = findBoundaries(dataset, featureName);
        this.featureName = featureName;
    }

    /**
     * Find all the boundary points within the provided dataset.
     * 
     * @param dataset The dataset, not <code>null</code>.
     * @return The values of the boundary points, each value denotes the beginning of a new bin, empty list in case no
     *         boundary points were found.
     */
    private static List<Double> findBoundaries(Iterable<? extends Instance> dataset, String featureName) {

        List<Instance> sortedData = CollectionHelper.newArrayList(dataset);
        Collections.sort(sortedData, new ValueComparator(featureName));

        List<Double> boundaries = CollectionHelper.newArrayList();

        int n = sortedData.size();
        CategoryEntries s = ClassificationUtils.getCategoryCounts(sortedData);
        double ent_s = ClassificationUtils.entropy(s);
        int k = s.size();

        for (int t = 1; t < n; t++) {
            String previousCategory = sortedData.get(t - 1).getCategory();
            String currentCategory = sortedData.get(t).getCategory();
            if (previousCategory.equals(currentCategory)) {
                continue;
            }

            CategoryEntries s1 = ClassificationUtils.getCategoryCounts(sortedData.subList(0, t));
            double ent_s1 = ClassificationUtils.entropy(s1);
            int k1 = s1.size();

            CategoryEntries s2 = ClassificationUtils.getCategoryCounts(sortedData.subList(t, n));
            double ent_s2 = ClassificationUtils.entropy(s2);
            int k2 = s2.size();

            double gain = ent_s - (double)t / n * ent_s1 - (double)(n - t) / n * ent_s2;
            double delta = log2(pow(3, k) - 2) - (k * ent_s - k1 * ent_s1 - k2 * ent_s2);
            LOGGER.trace("t={}, gain={}, delta={}", t, gain, delta);

            if (gain > log2(n - 1) / n + delta / n) {
                Value value = sortedData.get(t).getVector().get(featureName);
                if (value instanceof NumericValue) {
                    double doubleValue = ((NumericValue)value).getDouble();
                    if (!boundaries.contains(doubleValue)) {
                        boundaries.add(doubleValue);
                    }
                }
            }
        }
        LOGGER.debug("# boundary points for {}: {}", featureName, boundaries.size());
        return boundaries;
    }

    /**
     * Get the bin for the given value.
     * 
     * @param value The value.
     * @return The bin for the value.
     */
    public int bin(double value) {
        int position = Collections.binarySearch(boundaries, value);
        return position < 0 ? -position - 1 : position + 1;
    }

    /**
     * Get the number of boundary points (i.e. numBoundaryPoints + 1 = numBins).
     * 
     * @return The number of boundary points.
     */
    public int getNumBoundaryPoints() {
        return boundaries.size();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(featureName).append('\t');
        stringBuilder.append("# ");
        stringBuilder.append(boundaries.size());
        stringBuilder.append('\t');
        boolean first = true;
        for (Double bin : boundaries) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('|');
            }
            stringBuilder.append(bin);
        }
        return stringBuilder.toString();
    }

}

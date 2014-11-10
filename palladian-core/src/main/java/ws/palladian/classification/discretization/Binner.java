package ws.palladian.classification.discretization;

import static java.lang.Math.pow;
import static ws.palladian.helper.math.MathHelper.log2;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.value.AbstractValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class Binner implements Iterable<Binner.Interval> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Binner.class);

    public static final class Interval extends AbstractValue implements NominalValue {

        private final double lowererBound;
        private final double upperBound;

        /**
         * @param lowererBound The lower bound, exclusive.
         * @param upperBound The upper bound, inclusive.
         */
        public Interval(double lowererBound, double upperBound) {
            Validate.isTrue(lowererBound <= upperBound, "lowerBound must be smaller/equal to upperBound");
            this.lowererBound = lowererBound;
            this.upperBound = upperBound;
        }

        @Override
        public String getString() {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('(').append(format.format(lowererBound));
            stringBuilder.append(',');
            stringBuilder.append(format.format(upperBound)).append(']');
            return stringBuilder.toString();
        }

        @Override
        public int hashCode() {
            return getString().hashCode();
        }

        @Override
        protected boolean equalsValue(Value value) {
            Interval other = (Interval)value;
            if (lowererBound != other.lowererBound) {
                return false;
            }
            if (upperBound != other.upperBound) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return getString();
        }

    }

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
            // push NullValues to the end
            if (!(value1 instanceof NumericValue)) {
                return !(value2 instanceof NumericValue) ? 0 : 1;
            }
            if (!(value2 instanceof NumericValue)) {
                return -1;
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
        List<Instance> sortedData = CollectionHelper.newArrayList(dataset);
        Collections.sort(sortedData, new ValueComparator(featureName));
        // exclude NullValues from boundary search
        int idx = 0;
        for (Instance instance : sortedData) {
            if (instance.getVector().get(featureName) == NullValue.NULL) {
                break;
            }
            idx++;
        }
        sortedData = sortedData.subList(0, idx);
        this.boundaries = findBoundaries(sortedData, featureName);
        this.featureName = featureName;
    }

    /**
     * Find all the boundary points within the provided dataset.
     * 
     * @param dataset The dataset, not <code>null</code>.
     * @return The values of the boundary points, each value denotes the beginning of a new bin, empty list in case no
     *         boundary points were found.
     */
    private static List<Double> findBoundaries(List<Instance> dataset, String featureName) {

        CategoryEntries categoryPriors = ClassificationUtils.getCategoryCounts(dataset);
        double entS = ClassificationUtils.entropy(categoryPriors);
        int k = categoryPriors.size();
        int n = dataset.size();

        double maxGain = 0;
        double currentBoundary = 0;
        int boundaryIdx = -1;

        // the counts which are constantly updated during the split iterations
        CountingCategoryEntriesBuilder b1 = new CountingCategoryEntriesBuilder();
        CountingCategoryEntriesBuilder b2 = new CountingCategoryEntriesBuilder().add(categoryPriors);

        for (int i = 1; i < n; i++) {
            Instance previousInstance = dataset.get(i - 1);
            String previousCategory = previousInstance.getCategory();
            double previousValue = ((NumericValue)previousInstance.getVector().get(featureName)).getDouble();
            double currentValue = ((NumericValue)dataset.get(i).getVector().get(featureName)).getDouble();

            CategoryEntries c1 = b1.add(previousCategory, 1).create();
            CategoryEntries c2 = b2.subtract(previousCategory, 1).create();

            if (previousValue < currentValue) {
                double entS1 = ClassificationUtils.entropy(c1);
                double entS2 = ClassificationUtils.entropy(c2);
                double ent = (double)i / n * entS1 + (double)(n - i) / n * entS2;
                double gain = entS - ent;
                double delta = log2(pow(3, k) - 2) - (k * entS - c1.size() * entS1 - c2.size() * entS2);
                boolean mdlpcCriterion = gain > (log2(n - 1) + delta) / n;

                if (mdlpcCriterion && gain > maxGain) {
                    maxGain = gain;
                    currentBoundary = (previousValue + currentValue) / 2;
                    boundaryIdx = i;
                }
            }
        }

        if (maxGain == 0) { // stop recursion
            return Collections.emptyList();
        }

        LOGGER.debug("cut point = {} @ {}, gain = {}", currentBoundary, boundaryIdx, maxGain);

        // search boundaries recursive; result: find[leftSplit], currentBoundary, find[rightSplit]
        List<Double> boundaries = CollectionHelper.newArrayList();
        boundaries.addAll(findBoundaries(dataset.subList(0, boundaryIdx), featureName));
        boundaries.add(currentBoundary);
        boundaries.addAll(findBoundaries(dataset.subList(boundaryIdx, n), featureName));
        return boundaries;
    }

    /**
     * Get the bin index for the given value.
     * 
     * @param value The value.
     * @return The bin for the value.
     */
    public int bin(double value) {
        int position = Collections.binarySearch(boundaries, value);
        return position < 0 ? -position - 1 : position;
    }

    /**
     * Get the bin for the given value.
     * 
     * @param value The value.
     * @return The bin for the value.
     */
    public Interval getBin(double value) {
        int index = bin(value);
        return getBinAtIdx(index);
    }

    private Interval getBinAtIdx(int index) {
        double lowererBound = index == 0 ? Double.NEGATIVE_INFINITY : boundaries.get(index - 1);
        double upperBound = index == boundaries.size() ? Double.POSITIVE_INFINITY : boundaries.get(index);
        return new Interval(lowererBound, upperBound);
    }

    @Override
    public Iterator<Interval> iterator() {
        return new AbstractIterator<Binner.Interval>() {
            int idx = 0;

            @Override
            protected Interval getNext() throws Finished {
                if (idx > getNumBoundaryPoints()) {
                    throw FINISHED;
                }
                return getBinAtIdx(idx++);
            }
        };
    }

    /**
     * Get the number of boundary points (i.e. numBoundaryPoints + 1 = numBins).
     * 
     * @return The number of boundary points.
     */
    public int getNumBoundaryPoints() {
        return boundaries.size();
    }

    /**
     * @return The values of the boundary points, or an empty {@link List} in case no boundary points exist.
     */
    public List<Double> getBoundaries() {
        return Collections.unmodifiableList(boundaries);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(featureName).append('\t');
        boolean first = true;
        for (Interval bin : this) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(',');
            }
            stringBuilder.append(bin);
        }
        return stringBuilder.toString();
    }

}

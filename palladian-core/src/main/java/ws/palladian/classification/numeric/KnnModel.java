package ws.palladian.classification.numeric;

import ws.palladian.classification.utils.Normalization;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.statistics.DatasetStatistics;
import ws.palladian.core.value.*;
import ws.palladian.helper.math.FatStats;

import java.io.Serializable;
import java.util.*;

/**
 * <p>
 * The model used by KNN classification algorithms. Like the {@link KnnClassifier}.
 * </p>
 *
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class KnnModel implements Model {

    /** Used for serializing objects of this class. Should only change if the attribute set of the class changes. */
    private static final long serialVersionUID = 2203790409168130472L;

    /** The labels and their index within the vectors. */
    private final List<String> labelsNumericFields;
    private final List<String> labelsTextualFields;

    /** null values can't be stored in double[] so we say that infinity is null. */
    private final boolean allowNumericNull;

    private final static DoubleValue INFINITY_NULL = new DoubleValue() {
        @Override
        public double getDouble() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public long getLong() {
            return Long.MAX_VALUE;
        }

        @Override
        public float getFloat() {
            return Float.POSITIVE_INFINITY;
        }

        @Override
        public int getInt() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Number getNumber() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public boolean isNull() {
            return false;
        }
    };

    /** Training examples which are used for classification. */
    private final List<TrainingExample> trainingExamples;

    /** The trained category names. */
    private final Set<String> categories;

    /**
     * An object carrying the information to normalize {@link FeatureVector}s based on the normalized
     * {@link #trainingExamples}.
     */
    private final Normalization normalization;

    /**
     * <p>
     * Creates a new unnormalized {@code KnnModel} based on a {@code List} of {@link Instance}s.
     * </p>
     *
     * @param trainingInstances The {@link Instance}s this model is based on.
     */
    KnnModel(Dataset trainingInstances, Normalization normalization) {
        this(trainingInstances, normalization, false);
    }

    KnnModel(Dataset trainingInstances, Normalization normalization, boolean allowNumericNull) {
        this.allowNumericNull = allowNumericNull;
        DatasetStatistics statistics = new DatasetStatistics(trainingInstances);
        this.labelsNumericFields = new ArrayList<>(trainingInstances.getFeatureInformation().getFeatureNamesOfType(NumericValue.class));
        this.labelsTextualFields = new ArrayList<>(trainingInstances.getFeatureInformation().getFeatureNamesOfType(ImmutableStringValue.class));
        this.categories = new HashSet<>(statistics.getCategoryStatistics().getValues());
        this.trainingExamples = initTrainingInstances(trainingInstances, normalization);
        this.normalization = normalization;
    }

    private List<TrainingExample> initTrainingInstances(Iterable<? extends Instance> instances, Normalization normalization) {
        List<TrainingExample> ret = new ArrayList<>();
        for (Instance instance : instances) {
            FeatureVector normalizedFeatureVector = normalization.normalize(instance.getVector());
            double[] numericVector = new double[labelsNumericFields.size()];
            for (int idx = 0; idx < labelsNumericFields.size(); idx++) {
                Value value = normalizedFeatureVector.get(labelsNumericFields.get(idx));
                if (value.isNull()) {
                    if (!allowNumericNull) {
                        throw new IllegalArgumentException("NullValues are not supported");
                    } else {
                        value = INFINITY_NULL;
                    }
                }
                NumericValue numericValue = (NumericValue) value;
                numericVector[idx] = numericValue.getDouble();
            }

            String[] textualVector = new String[labelsTextualFields.size()];
            for (int idx = 0; idx < labelsTextualFields.size(); idx++) {
                Value value = instance.getVector().get(labelsTextualFields.get(idx));
                if (value.isNull()) {
                    textualVector[idx] = "";
                } else {
                    ImmutableStringValue textValue = (ImmutableStringValue) value;
                    textualVector[idx] = textValue.getString();
                }
            }

            ret.add(new TrainingExample(numericVector, textualVector, instance.getCategory()));
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("KnnModel [");
        toStringBuilder.append("# trainingInstances=").append(trainingExamples.size());
        toStringBuilder.append(" normalization=").append(normalization);
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }

    @Override
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    List<TrainingExample> getTrainingExamples() {
        return Collections.unmodifiableList(trainingExamples);
    }

    String[] getStringVectorForClassification(FeatureVector vector) {
        Objects.requireNonNull(vector, "vector must not be null");

        int numNonNumericFeatures = labelsTextualFields.size();
        String[] stringVector = new String[numNonNumericFeatures];
        for (int idx = 0; idx < numNonNumericFeatures; idx++) {
            Value value = vector.get(labelsTextualFields.get(idx));
            if (value.isNull()) {
                stringVector[idx] = "";
            } else {
                if (value instanceof TextValue) {
                    stringVector[idx] = ((TextValue) value).getText();
                } else if (value instanceof ImmutableStringValue) {
                    stringVector[idx] = ((ImmutableStringValue) value).getString();
                } else {
                    throw new IllegalArgumentException(
                            "Expected value " + labelsTextualFields.get(idx) + " to be of type " + NumericValue.class + ", but was " + value.getClass() + " (" + value + ")");
                }
            }
        }
        return stringVector;
    }

    double[] getNormalizedVectorForClassification(FeatureVector vector) {
        Objects.requireNonNull(vector, "vector must not be null");
        //        FeatureVector normalizedFeatureVector = normalization.normalize(vector);
        double[] numericVector = new double[labelsNumericFields.size()];
        for (int idx = 0; idx < labelsNumericFields.size(); idx++) {
            Value value = vector.get(labelsNumericFields.get(idx));
            if (value.isNull()) {
                if (!allowNumericNull) {
                    throw new IllegalArgumentException("NullValues are not supported");
                } else {
                    value = INFINITY_NULL;
                }
            }
            if (value instanceof NumericValue) {
                NumericValue numericValue = (NumericValue) value;
                double normalizedValue = normalization.normalize(labelsNumericFields.get(idx), numericValue.getDouble());
                numericVector[idx] = normalizedValue;
            } else {
                throw new IllegalArgumentException(
                        "Expected value " + labelsNumericFields.get(idx) + " to be of type " + NumericValue.class + ", but was " + value.getClass() + " (" + value + ")");
            }
            //			NumericValue value = (NumericValue) normalizedFeatureVector.get(labels.get(idx));
            //			numericVector[idx] = value.getDouble();
        }
        return numericVector;
    }

}

final class TrainingExample implements Serializable {
    private static final long serialVersionUID = -2340565652781930965L;
    final double[] numericFeatures;
    final String[] textualFeatures;
    final String category;

    public TrainingExample(double[] features, String[] textualFeatures, String category) {
        this.numericFeatures = features;
        this.textualFeatures = textualFeatures;
        this.category = category;
    }

    /**
     * The Euclidean distance to the given vector.
     *
     * @param otherNumeric The vector, not <code>null</code>.
     * @param otherTextual The vector, not <code>null</code>.
     * @return The distance.
     */
    public double distance(double[] otherNumeric, String[] otherTextual) {
        Objects.requireNonNull(otherNumeric, "otherNumeric must not be null");
        Objects.requireNonNull(otherTextual, "otherTextual must not be null");
        if (numericFeatures.length != otherNumeric.length) {
            throw new IllegalArgumentException("length of given vector must be " + numericFeatures.length + ", but was " + otherNumeric.length);
        }
        if (textualFeatures.length != otherTextual.length) {
            throw new IllegalArgumentException("length of given vector must be " + textualFeatures.length + ", but was " + otherTextual.length);
        }
        double distance = 0;
        FatStats stats = new FatStats();
        for (int idx = 0; idx < this.numericFeatures.length; idx++) {
            if (Double.isInfinite(this.numericFeatures[idx]) || Double.isInfinite(otherNumeric[idx])) {
                continue;
            }
            double value = this.numericFeatures[idx] - otherNumeric[idx];
            distance += value * value;
            stats.add(Math.abs(value));
        }

        // XXX
        // the idea is to make the penalty for non-matches relative to the difference in numeric values
        // this is now a different value per sample which doesn't really make sense but evaluation shows it works better than static values
        double penalty = stats.getCount() > 0 ? Math.max(1, stats.getMax()) : 1;
        for (int idx = 0; idx < this.textualFeatures.length; idx++) {
            double value = penalty;
            if (this.textualFeatures[idx].equalsIgnoreCase(otherTextual[idx])) {
                value = 0;
            }
            distance += value * value;
        }
        return distance;
    }

    public double distance(double[] other) {
        Objects.requireNonNull(other, "other must not be null");
        if (numericFeatures.length != other.length) {
            throw new IllegalArgumentException("length of given vector must be " + numericFeatures.length + ", but was " + other.length);
        }
        double distance = 0;
        for (int idx = 0; idx < this.numericFeatures.length; idx++) {
            double value = this.numericFeatures[idx] - other[idx];
            distance += value * value;
        }
        return distance;
    }

    @Override
    public String toString() {
        return category + ":" + Arrays.toString(numericFeatures);
    }
}

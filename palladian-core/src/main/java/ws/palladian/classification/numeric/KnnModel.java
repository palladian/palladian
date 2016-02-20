package ws.palladian.classification.numeric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ws.palladian.classification.discretization.DatasetStatistics;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

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
    private final List<String> labels;
    
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
    KnnModel(Iterable<? extends Instance> trainingInstances, Normalization normalization) {
    	this.labels = getLabels(trainingInstances);
    	this.categories = getCategories(trainingInstances);
        this.trainingExamples = initTrainingInstances(trainingInstances, normalization, labels);
        this.normalization = normalization;
    }

    private static Set<String> getCategories(Iterable<? extends Instance> trainingInstances) {
    	return new DatasetStatistics(trainingInstances).getCategoryPriors().getNames();
	}

	private static List<String> getLabels(Iterable<? extends Instance> trainingInstances) {
    	Instance firstInstance = trainingInstances.iterator().next();
    	List<String> lables = new ArrayList<>();
    	for (VectorEntry<String, Value> entry : firstInstance.getVector()) {
			if (entry.value() instanceof NumericValue) {
				lables.add(entry.key());
			}
		}
    	return lables;
	}

	private static List<TrainingExample> initTrainingInstances(Iterable<? extends Instance> instances,
            Normalization normalization, List<String> labels) {
        List<TrainingExample> ret = new ArrayList<>();
        for (Instance instance : instances) {
            FeatureVector normalizedFeatureVector = normalization.normalize(instance.getVector());
            double[] vector = new double[labels.size()];
            for (int idx = 0; idx < labels.size(); idx++) {
				NumericValue value = (NumericValue) normalizedFeatureVector.get(labels.get(idx));
				vector[idx] = value.getDouble();
			}
            ret.add(new TrainingExample(vector, instance.getCategory()));
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
	
	double[] getNormalizedVectorForClassification(FeatureVector vector) {
		Objects.requireNonNull(vector, "vector must not be null");
//        FeatureVector normalizedFeatureVector = normalization.normalize(vector);
		double[] numericVector = new double[labels.size()];
		for (int idx = 0; idx < labels.size(); idx++) {
			Value value = vector.get(labels.get(idx));
			if (value instanceof NumericValue) {
				NumericValue numericValue = (NumericValue) value;
				double normalizedValue = normalization.normalize(labels.get(idx), numericValue.getDouble());
				numericVector[idx] = normalizedValue;
			} else {
				throw new IllegalArgumentException("Expected value " + labels.get(idx) + " to be of type "
						+ NumericValue.class + ", but was " + value.getClass() + " (" + value + ")");
			}
//			NumericValue value = (NumericValue) normalizedFeatureVector.get(labels.get(idx));
//			numericVector[idx] = value.getDouble();
		}
		return numericVector;
	}

}

final class TrainingExample implements Serializable {
	private static final long serialVersionUID = -2340565652781930965L;
	final double[] features;
	final String category;

	public TrainingExample(double[] features, String category) {
		this.features = features;
		this.category = category;
	}

	/**
	 * The Euclidean distance to the given vector.
	 * 
	 * @param other
	 *            The vector, not <code>null</code>.
	 * @return The distance.
	 */
	public double distance(double[] other) {
		Objects.requireNonNull(other, "other must not be null");
		if (features.length != other.length) {
			throw new IllegalArgumentException(
					"length of given vector must be " + features.length + ", but was " + other.length);
		}
		double distance = 0;
		for (int idx = 0; idx < this.features.length; idx++) {
			double value = this.features[idx] - other[idx];
			distance += value * value;
		}
		return distance;
	}

	@Override
	public String toString() {
		return category + ":" + Arrays.toString(features);
	}
}

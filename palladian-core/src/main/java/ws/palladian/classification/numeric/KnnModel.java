package ws.palladian.classification.numeric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ws.palladian.classification.utils.Normalization;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Model;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.math.ImmutableNumericVector;
import ws.palladian.helper.math.NumericVector;

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
    private static final long serialVersionUID = -6528509220813706056L;

    /** Training examples which are used for classification. */
    private final List<TrainingExample> trainingExamples;

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
        this.trainingExamples = initTrainingInstances(trainingInstances, normalization);
        this.normalization = normalization;
    }

    private List<TrainingExample> initTrainingInstances(Iterable<? extends Instance> instances,
            Normalization normalization) {
        List<TrainingExample> ret = CollectionHelper.newArrayList();
        for (Instance instance : instances) {
            FeatureVector normalizedFeatureVector = normalization.normalize(instance.getVector());
            ret.add(new TrainingExample(normalizedFeatureVector, instance.getCategory()));
        }
        return ret;
    }

    /**
     * @return The training instances underlying this {@link KnnModel}. They are used by the {@code KnnClassifier} to
     *         make a classification decision.
     */
    public List<TrainingExample> getTrainingExamples() {
        return trainingExamples;
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
        Set<String> categories = CollectionHelper.newHashSet();
        for (TrainingExample example : trainingExamples) {
            categories.add(example.category);
        }
        return categories;
    }

    public Normalization getNormalization() {
        return normalization;
    }

}

class TrainingExample implements Serializable {
    private static final long serialVersionUID = 6007693177447711704L;
    final String category;
    final Map<String, Double> features;

    public TrainingExample(FeatureVector featureVector, String category) {
        this.category = category;
        features = new HashMap<String, Double>();
        for (VectorEntry<String, Value> entry : featureVector) {
            Value value = entry.value();
            if (value instanceof NumericValue) {
                features.put(entry.key(), ((NumericValue)entry.value()).getDouble());
            }
        }
    }
    
    public NumericVector<String> getVector() {
        return new ImmutableNumericVector<String>(features);
    }

    @Override
    public String toString() {
        return category + ":" + features;
    }
}

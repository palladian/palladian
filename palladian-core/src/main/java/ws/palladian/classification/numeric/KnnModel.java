package ws.palladian.classification.numeric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.classification.Instance;
import ws.palladian.classification.Model;
import ws.palladian.classification.utils.MinMaxNormalization;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVectorImpl;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * The model used by KNN classification algorithms. Like the {@link KnnClassifier}.
 * </p>
 * 
 * @author Klemens Muthmann
 */
public final class KnnModel implements Model {

    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of the class changes.
     * </p>
     */
    private static final long serialVersionUID = -6528509220813706056L;
    /**
     * <p>
     * Training examples which are used for classification.
     * </p>
     */
    private List<TrainingExample> trainingExamples;

    /**
     * <p>
     * Whether this is a normalized {@code KnnModel} or not.
     * </p>
     */
    private boolean isNormalized;
    /**
     * <p>
     * An object carrying the information to normalize {@link FeatureVector}s based on the normalized
     * {@link #trainingExamples}.
     * </p>
     */
    private MinMaxNormalization normalizationInformation;

    /**
     * <p>
     * Creates a new unnormalized {@code KnnModel} based on a {@code List} of {@link Instance}s.
     * </p>
     * 
     * @param trainingInstances The {@link Instance}s this model is based on.
     */
    public KnnModel(Iterable<? extends Trainable> trainingInstances) {
        this.trainingExamples = initTrainingInstances(trainingInstances);
        this.isNormalized = false;
    }

    private List<TrainingExample> initTrainingInstances(Iterable<? extends Trainable> instances) {
        List<TrainingExample> ret = new ArrayList<TrainingExample>();
        for (Trainable instance : instances) {
            TrainingExample trainingInstance = new TrainingExample();

            trainingInstance.targetClass = instance.getTargetClass();
            trainingInstance.features = new HashMap<String, Double>();
            List<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);
            for (NumericFeature feature : numericFeatures) {
                trainingInstance.features.put(feature.getName(), feature.getValue());
            }

            ret.add(trainingInstance);
        }
        return ret;
    }

    /**
     * @return The training instances underlying this {@link KnnModel}. They are used by the {@code KnnClassifier} to
     *         make a classification decision.
     */
    public List<Trainable> getTrainingExamples() {
        return convertTrainingInstances(trainingExamples);
    }

    private List<Trainable> convertTrainingInstances(List<TrainingExample> instances) {
        List<Trainable> nominalInstances = new ArrayList<Trainable>(instances.size());

        for (TrainingExample instance : trainingExamples) {
            FeatureVector featureVector = new BasicFeatureVectorImpl();
            for (Entry<String, Double> feature : instance.features.entrySet()) {
                featureVector.add(new NumericFeature(feature.getKey(), feature.getValue()));
            }
            Instance nominalInstance = new Instance(instance.targetClass, featureVector);
            nominalInstances.add(nominalInstance);
        }

        return nominalInstances;
    }

    /**
     * <p>
     * Min max normalizes all {@link Instance}s of this model.
     * </p>
     */
    public void normalize() {
        List<Trainable> nominalInstances = convertTrainingInstances(trainingExamples);
        normalizationInformation = new MinMaxNormalization(nominalInstances);
        normalizationInformation.normalize(nominalInstances);
        trainingExamples = initTrainingInstances(nominalInstances);
        isNormalized = true;
    }

    /**
     * <p>
     * Normalizes a {@link FeatureVector} based on the {@link Instance} within this model. A call to this method makes
     * only sense if the model was previously normalized using {@link #normalize()}. Otherwise it throws an
     * {@code IllegalStateException}.
     * </p>
     * 
     * @param vector The {@link FeatureVector} to normalize.
     */
    public void normalize(FeatureVector vector) {
        if (!isNormalized) {
            throw new IllegalStateException(
                    "Tried calling normalize for an unnormalized model. Please normalize this model before you try this again.");
        }
        normalizationInformation.normalize(vector);
    }

    /**
     * @return {@code true} if this model is normalized; {@code false} otherwise.
     */
    public boolean isNormalized() {
        return isNormalized;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("KnnModel [");
        toStringBuilder.append("# trainingInstances=").append(trainingExamples.size());
        toStringBuilder.append(", isNormalized=").append(isNormalized);
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }
}

class TrainingExample implements Serializable {
    private static final long serialVersionUID = 6007693177447711704L;
    String targetClass;
    Map<String, Double> features;
}

/**
 * 
 */
package ws.palladian.classification.numeric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.classification.ClassificationUtils;
import ws.palladian.classification.Model;
import ws.palladian.classification.NominalInstance;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * The model used by KNN classification algorithms. Like the {@link KnnClassifier}.
 * </p>
 * 
 * @author Klemens Muthmann
 */
public final class KnnModel implements Model, Serializable {

    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of the class changes.
     * </p>
     */
    private static final long serialVersionUID = -6528509220813706056L;
    /**
     * <p>
     * Non-transient training instances. We need to save them as the instance based classifier depends on them.
     * </p>
     */
    private List<TrainingInstance> trainingInstances;

    /**
     * <p>
     * Whether this is a normalized {@code KnnModel} or not.
     * </p>
     */
    private Boolean isNormalized;
    /**
     * <p>
     * An object carrying the information to normalize {@link FeatureVector}s based on the normalized
     * {@link #trainingInstances}.
     * </p>
     */
    private MinMaxNormalization normalizationInformation;

    /**
     * <p>
     * Creates a new unnormalized {@code KnnModel} based on a {@code List} of {@link NominalInstance}s.
     * </p>
     * 
     * @param trainingInstances The {@link NominalInstance}s this model is based on.
     */
    public KnnModel(List<NominalInstance> trainingInstances) {
        super();

        this.trainingInstances = initTrainingInstances(trainingInstances);
        this.isNormalized = false;
    }

    private List<TrainingInstance> initTrainingInstances(List<NominalInstance> trainingInstances2) {
        List<TrainingInstance> ret = new ArrayList<TrainingInstance>(trainingInstances2.size());
        for(NominalInstance instance: trainingInstances2) {
            TrainingInstance trainingInstance = new TrainingInstance();
            
            trainingInstance.targetClass = instance.targetClass;
            trainingInstance.features = new HashMap<String,Double>();
            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);
            for(Feature<Double> feature:numericFeatures) {
                trainingInstance.features.put(feature.getName(), feature.getValue());
            }
            
            ret.add(trainingInstance);
        }
        return ret;
    }

    /**
     * @return The training instances underlying this {@link KnnModel}. They are
     *         used by the {@code KnnClassifier} to make a classification
     *         decision.
     */
    public List<NominalInstance> getTrainingInstances() {
        return convertTrainingInstances(trainingInstances);
    }

    private List<NominalInstance> convertTrainingInstances(List<TrainingInstance> trainingInstances2) {
        List<NominalInstance> nominalInstances = new ArrayList<NominalInstance>(trainingInstances2.size());
        
        for(TrainingInstance trainingInstance:trainingInstances) {
            NominalInstance nominalInstance = new NominalInstance();
            nominalInstance.targetClass = trainingInstance.targetClass;
            nominalInstance.featureVector = new FeatureVector();
            for(Entry<String, Double> feature:trainingInstance.features.entrySet()) {
                nominalInstance.featureVector.add(new NumericFeature(FeatureDescriptorBuilder.build(feature.getKey(), NumericFeature.class), feature.getValue()));
            }
            nominalInstances.add(nominalInstance);
        }
        
        return nominalInstances;
    }

    /**
     * <p>
     * Min max normalizes all {@link NominalInstance}s of this model.
     * </p>
     */
    public void normalize() {
        List<NominalInstance> nominalInstances = convertTrainingInstances(trainingInstances);
        normalizationInformation = ClassificationUtils.minMaxNormalize(nominalInstances);
        trainingInstances = initTrainingInstances(nominalInstances);
        isNormalized = true;
    }

    /**
     * <p>
     * Normalizes a {@link FeatureVector} based on the {@link NominalInstance} within this model. A call to this method
     * makes only sense if the model was previously normalized using {@link #normalize()}. Otherwise it throws an
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

        List<Feature<Double>> features = vector.getAll(Double.class);

        for (Feature<Double> feature:features) {
            String featureName = feature.getName();
            double featureValue = feature.getValue();
            double normalizedValue = (featureValue - normalizationInformation.getMinValueMap().get(featureName))
                    / normalizationInformation.getNormalizationMap().get(featureName);

            feature.setValue(normalizedValue);
        }

    }

    /**
     * @return {@code true} if this model is normalized; {@code false} otherwise.
     */
    public Boolean isNormalized() {
        return Boolean.valueOf(isNormalized);
    }
}

class TrainingInstance implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 6007693177447711704L;
    String targetClass;
    Map<String,Double> features;
}

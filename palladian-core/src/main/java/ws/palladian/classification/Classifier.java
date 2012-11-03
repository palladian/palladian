package ws.palladian.classification;

import java.util.List;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * A classifier uses a trained model to classify an instance into a category.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * 
 * @param <M> The type of the model that is used to classify an instance.
 */
public interface Classifier<M extends Model> {

    M train(List<Instance> instances);

    M train(Dataset dataset);

    CategoryEntries classify(FeatureVector vector, M model);

}
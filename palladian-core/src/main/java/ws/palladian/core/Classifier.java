package ws.palladian.core;

/**
 * <p>
 * A classifier uses a trained model to classify a feature vector into a category.
 * </p>
 *
 * @param <M> The type of the model that is used to classify an instance.
 * @author David Urbansky
 * @author Philipp Katz
 */
public interface Classifier<M extends Model> {

    /**
     * <p>
     * Classify an object with the given model.
     * </p>
     *
     * @param featureVector The feature vector to classify.
     * @param model         The model to use for the classification.
     * @return The classification result.
     */
    CategoryEntries classify(FeatureVector featureVector, M model);

}

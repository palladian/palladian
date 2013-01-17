package ws.palladian.classification;

import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;

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

    /**
     * <p>
     * Train a model for the given training data.
     * </p>
     * 
     * @param trainables The training data to use for building the model.
     * @return The model for the given training data.
     */
    M train(Iterable<? extends Trainable> trainables);

    /**
     * <p>
     * Classify an object with the given model.
     * </p>
     * 
     * @param classifiable The object to classify.
     * @param model The model to use for the classification.
     * @return The classification result.
     */
    CategoryEntries classify(Classifiable classifiable, M model);

}

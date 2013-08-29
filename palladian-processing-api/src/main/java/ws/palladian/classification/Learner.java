package ws.palladian.classification;

import ws.palladian.processing.Trainable;

/**
 * <p>
 * A learner which creates a {@link Model} from {@link Trainable} data for prediction. The created model can used for
 * classification through a {@link Classifier}.
 * 
 * @author Philipp Katz
 * 
 * @param <M> The type of the model that is created.
 */
public interface Learner<M extends Model> {

    /**
     * <p>
     * Train a model for the given training data.
     * </p>
     * 
     * @param trainables The training data to use for building the model.
     * @return The model for the given training data.
     */
    M train(Iterable<? extends Trainable> trainables);

}

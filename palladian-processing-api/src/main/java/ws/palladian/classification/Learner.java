package ws.palladian.classification;

import ws.palladian.processing.Trainable;

/**
 * <p>
 * A learner which creates a {@link Model} from {@link Trainable} data for prediction. The created model can used for
 * classification through a {@link Classifier}.
 * 
 * @author Philipp Katz
 */
public interface Learner {

    /**
     * <p>
     * Train a model for the given training data.
     * </p>
     * 
     * @param trainables The training data to use for building the model.
     * @return The model for the given training data.
     */
    Model train(Iterable<? extends Trainable> trainables);

}

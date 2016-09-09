package ws.palladian.core;

import ws.palladian.core.dataset.Dataset;

/**
 * <p>
 * A learner which creates a {@link Model} from {@link Instance} data for prediction. The created model can used for
 * classification through a {@link Classifier}.
 * 
 * @author Philipp Katz
 * 
 * @param <M> The type of the model that is created.
 */
public interface Learner<M extends Model> {

    /**
     * <p>
     * Train a model from the given training data.
     * </p>
     * 
     * @param instances The training data to use for building the model.
     * @return The model for the given training data.
     * @deprecated Use {@link #train(Dataset)} instead.
     */
	@Deprecated
    M train(Iterable<? extends Instance> instances);
    
	/**
	 * Train a model from the given training data.
	 * 
	 * @param dataset
	 *            The dataset for building the model.
	 * @return The model.
	 */
	M train(Dataset dataset);
	
	/**
	 * Train a model from the given training data and use a separately supplied
	 * dataset for validation during the training process, e.g. for early
	 * stopping. Learner implementations which do not support this, simply
	 * ignore the validation data.
	 * 
	 * @param training
	 *            The dataset for building the model.
	 * @param validation
	 *            The dataset for validating, <code>null</code> in case there is
	 *            no dedicated validation set (functionally equivalent to
	 *            calling {@link #train(Dataset)}).
	 * @return The model.
	 */
	M train(Dataset training, Dataset validation);

}

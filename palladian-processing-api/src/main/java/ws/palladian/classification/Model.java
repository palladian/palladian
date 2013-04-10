package ws.palladian.classification;

import java.io.Serializable;

/**
 * <p>
 * This interface describes a model used for classifiers. Concrete classifier implementations may add logic for keeping
 * data here after the classifier has been trained. This class inherits from {@link Serializable} to allow for
 * persisting of models. Take care which data needs to be saved, and try to keep backwards compatibility when you
 * perform any modifications.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Model extends Serializable {

}

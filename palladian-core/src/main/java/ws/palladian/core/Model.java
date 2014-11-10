package ws.palladian.core;

import java.io.Serializable;
import java.util.Set;

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

    /**
     * @return The categories supported by this Model. These are usually the categories (classes) on which the model was
     *         trained.
     */
    Set<String> getCategories();

}

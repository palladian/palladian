/**
 * 
 */
package ws.palladian.processing;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Basic interface for objects that are classifiable by any subclass of {@link Classifier}. Any implementation needs to
 * supply at least a {@link FeatureVector} providing the {@link Feature}s to use for classification. If the
 * {@code FeatureVector} does not contain {@code Feature}s processable by that classifier the classifier will complain
 * by throwing an appropriate {@code Exception}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public interface Classifiable {
    /**
     * @return The {@link FeatureVector} provided by this {@code Classifiable} object.
     */
    FeatureVector getFeatureVector();

}

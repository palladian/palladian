/**
 * Created on: 24.06.2013 13:06:53
 */
package ws.palladian.processing.features;

import java.util.List;

/**
 * <p>
 * A {@code ListFeature} groups features belonging to the same type such as tokens from a document. {@code ListFeature}s
 * are usually sparse features.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 * @param <T>
 */
public final class ListFeature<T> extends AbstractFeature<List<T>> {

    /**
     * <p>
     * Creates a new completely initialized {@link ListFeature} with the provided name and value.
     * </p>
     * 
     * @param name The name of the new {@link ListFeature}. This is later used to retrieve this feature from a
     *            {@link FeatureVector}.
     * @param value The value of this {@link ListFeature}.
     */
    public ListFeature(String name, List<T> value) {
        super(name, value);

    }
    //
    // /**
    // * <p>
    // * Removes the provided value from this {@link ListFeature}.
    // * </p>
    // *
    // * @param valueToRemove The value to remove.
    // */
    // public void remove(final T valueToRemove) {
    // Validate.notNull(valueToRemove);
    //
    // getValue().remove(valueToRemove);
    // }

}

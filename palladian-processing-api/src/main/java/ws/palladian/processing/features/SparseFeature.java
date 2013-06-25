package ws.palladian.processing.features;


/**
 * <p>
 * This class handles sparse features that have no value other than being present or not. Such features are for example
 * tokens, n-grams etc. For such features the features name is a {@link String} representation of its value.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.2.2
 * @version 1.0
 */
public final class SparseFeature<T> implements Feature<T> {

    /**
     * <p>
     * The value of this feature.
     * </p>
     */
    private T value;
    
    /**
     * <p>
     * Creates a new completely initialized {@link SparseFeature}.
     * </p>
     * 
     * @param value The value of the new {@link SparseFeature}. Its {@link String} representation is also its name, that you get via {@link #getName()}.
     */
    public SparseFeature(T value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return value.toString();
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(T value) {
        throw new UnsupportedOperationException(
                "Cannot change the value of an existing sparse feature. Please create a new object if you have a sparse feature with another value.");
    }

}

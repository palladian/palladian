package ws.palladian.helper.collection;

/**
 * <p>
 * A filter predicate. Implementations decide within the {@link #accept(Object)} method, what to do with the item.
 * </p>
 * 
 * @author Philipp Katz
 * @param <T> Type of the object to filter.
 * @see CollectionHelper
 */
public interface Filter<T> {

    /**
     * <p>
     * A filter which removes <code>null</code> elements.
     * </p>
     * 
     * @see CollectionHelper#removeNulls(Iterable)
     */
    public static final Filter<Object> NULL_FILTER = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return item != null;
        }
    };

    /**
     * <p>
     * A filter which accepts all elements.
     * </p>
     */
    public static final Filter<Object> ACCEPT = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return true;
        }
    };

    /**
     * <p>
     * A filter which rejects all elements.
     * </p>
     */
    public static final Filter<Object> REJECT = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return false;
        }
    };

    boolean accept(T item);

}

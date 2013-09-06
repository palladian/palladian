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
    public static Filter<Object> NULL_FILTER = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return item != null;
        }
    };

    boolean accept(T item);

}

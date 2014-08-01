package ws.palladian.helper.functional;

/**
 * <p>
 * A filter predicate. Implementations decide within the {@link #accept(Object)} method, what to do with the item.
 * </p>
 * 
 * @author Philipp Katz
 * @param <T> Type of the object to filter.
 * @see Filters for default implementations.
 */
public interface Filter<T> {

    boolean accept(T item);

}

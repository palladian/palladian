package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Filter;

/**
 * <p>
 * A {@link Filter} decorator with inverse logic. Items which would be accepted by the wrapped Filter are discarded, and
 * vice versa.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T> Type of items to filter.
 */
public final class InverseFilter<T> implements Filter<T> {

    private final Filter<T> filter;

    /**
     * <p>
     * Create a new {@link InverseFilter} wrapping the specified {@link Filter}.
     * </p>
     * 
     * @param filter The Filter to wrap, not <code>null</code>.
     * @return A filter with inverted logic of the specified filter.
     */
    public static <T> InverseFilter<T> create(Filter<T> filter) {
        return new InverseFilter<T>(filter);
    }

    private InverseFilter(Filter<T> filter) {
        Validate.notNull(filter, "filter must not be null");
        this.filter = filter;
    }

    @Override
    public boolean accept(T item) {
        return !filter.accept(item);
    }

}

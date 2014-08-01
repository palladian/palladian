package ws.palladian.helper.collection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.helper.functional.Filter;

/**
 * <p>
 * A chain of {@link Filter}s effectively acting as an AND filter, i.e. the processed items need to pass all contained
 * filters, to be accepted by the chain.
 * </p>
 * 
 * @param <T> Type of items to be processed.
 * @author pk
 */
public final class FilterChain<T> implements Filter<T> {

    private final Set<Filter<? super T>> filters;

    public FilterChain(Set<Filter<? super T>> filters) {
        this.filters = filters;
    }

    public FilterChain(Filter<? super T>... filters) {
        this.filters = new HashSet<Filter<? super T>>(Arrays.asList(filters));
    }

    @Override
    public boolean accept(T item) {
        for (Filter<? super T> filter : filters) {
            if (!filter.accept(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FilterChain [filters=");
        builder.append(filters);
        builder.append("]");
        return builder.toString();
    }

}

package ws.palladian.helper.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.helper.functional.Filter;

/**
 * <p>
 * A {@link Filter} which simply filters by Object's equality ({@link Object#equals(Object)}).
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T> The type of items to filter.
 */
public class EqualsFilter<T> implements Filter<T> {

    private final Set<T> values;

    public static <T> EqualsFilter<T> create(T value) {
        return new EqualsFilter<T>(Collections.singleton(value));
    }

    public static <T> EqualsFilter<T> create(Collection<T> values) {
        return new EqualsFilter<T>(new HashSet<T>(values));
    }

    public static <T> EqualsFilter<T> create(T... values) {
        return new EqualsFilter<T>(new HashSet<T>(Arrays.asList(values)));
    }

    private EqualsFilter(Set<T> values) {
        this.values = values;
    }

    @Override
    public boolean accept(T item) {
        return item != null && values.contains(item);
    }

}

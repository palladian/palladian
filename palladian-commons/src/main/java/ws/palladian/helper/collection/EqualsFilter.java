package ws.palladian.helper.collection;

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

    private final T value;

    public static <T> EqualsFilter<T> create(T value) {
        return new EqualsFilter<T>(value);
    }

    private EqualsFilter(T value) {
        this.value = value;
    }

    @Override
    public boolean accept(T item) {
        return item != null && item.equals(value);
    }

}

package ws.palladian.helper.collection;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

/**
 * {@link Filter} which accepts all items which are contained within a given {@link Collection}.
 * 
 * @author pk
 * 
 * @param <T>
 */
public final class ContainsFilter<T> implements Filter<T> {

    private final Collection<T> collection;

    public static <T> ContainsFilter<T> create(Collection<T> collection) {
        Validate.notNull(collection, "collection must not be null");
        return new ContainsFilter<T>(collection);
    }

    private ContainsFilter(Collection<T> collection) {
        this.collection = collection;
    }

    @Override
    public boolean accept(T item) {
        return collection.contains(item);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ContainsFilter [collection=");
        builder.append(collection);
        builder.append("]");
        return builder.toString();
    }

}

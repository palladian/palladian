package ws.palladian.helper.collection;

import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.helper.collection.CollectionHelper.Order;

import java.util.Comparator;
import java.util.Map.Entry;

/**
 * <p>
 * Compare {@link Entry}s or {@link Pair}s by their values. Use the static factory methods {@link #ascending()} or
 * {@link #descending()} to obtain an instance: <code>
 * Collections.sort(list, EntryValueComparator.<K, V> ascending())
 * </code>
 * </p>
 *
 * @param <V> Entry value (must implement {@link Comparable}).
 * @author Philipp Katz
 */
public final class EntryValueComparator<V extends Comparable<V>> implements Comparator<Entry<?, V>> {
    private final Order order;

    public EntryValueComparator(Order order) {
        this.order = order;
    }

    @Override
    public int compare(Entry<?, V> e1, Entry<?, V> e2) {
        return (order == Order.ASCENDING ? 1 : -1) * e1.getValue().compareTo(e2.getValue());
    }
}

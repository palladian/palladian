package ws.palladian.helper.collection;

import java.util.Comparator;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * Compare {@link Entry}s or {@link Pair}s by their values. Use the static factory methods {@link #ascending()} or
 * {@link #descending()} to obtain an instance: <code>
 * Collections.sort(list, EntryValueComparator.<K, V> ascending())
 * </code>
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Entry key.
 * @param <V> Entry value (must implement {@link Comparable}).
 */
public final class EntryValueComparator<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {

    private final boolean ascending;

    /**
     * <p>
     * Create an {@link EntryValueComparator} for sorting in an ascending way.
     * </p>
     * 
     * @return The comparator.
     */
    public static <K, V extends Comparable<V>> EntryValueComparator<K, V> ascending() {
        return new EntryValueComparator<K, V>(true);
    }

    /**
     * <p>
     * Create an {@link EntryValueComparator} for sorting in a descending way.
     * </p>
     * 
     * @return The comparator.
     */
    public static <K, V extends Comparable<V>> EntryValueComparator<K, V> descending() {
        return new EntryValueComparator<K, V>(false);
    }

    private EntryValueComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Entry<K, V> e1, Entry<K, V> e2) {
        return (ascending ? 1 : -1) * e1.getValue().compareTo(e2.getValue());
    }

}

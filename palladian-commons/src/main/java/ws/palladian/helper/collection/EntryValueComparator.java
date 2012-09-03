package ws.palladian.helper.collection;

import java.util.Comparator;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * Compare {@link Entry}s or {@link Pair}s by their values.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Entry key.
 * @param <V> Entry value (must implement {@link Comparable}).
 */
public final class EntryValueComparator<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {

    private final boolean ascending;

    public EntryValueComparator(boolean ascending) {
        this.ascending = ascending;
    }

    public EntryValueComparator() {
        this(true);
    }

    @Override
    public int compare(Entry<K, V> e1, Entry<K, V> e2) {
        return (ascending ? 1 : -1) * e1.getValue().compareTo(e2.getValue());
    }

}

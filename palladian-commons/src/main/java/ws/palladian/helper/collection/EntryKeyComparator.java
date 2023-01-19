package ws.palladian.helper.collection;

import java.util.Comparator;
import java.util.Map.Entry;

/**
 * Compares two {@link Entry}s by their keys (keys may also be null).
 *
 * @author Philipp Katz
 */
public final class EntryKeyComparator<K extends Comparable<K>> implements Comparator<Entry<K, ?>> {
    @Override
    public int compare(Entry<K, ?> e1, Entry<K, ?> e2) {
        if (e1 == null && e2 == null) {
            return 0;
        }
        if (e1 == null || e2 == null) {
            return e1 == null ? -1 : 1;
        }
        return e1.getKey().compareTo(e2.getKey());
    }
}
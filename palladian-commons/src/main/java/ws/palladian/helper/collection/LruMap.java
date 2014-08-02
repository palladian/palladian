package ws.palladian.helper.collection;

import java.util.LinkedHashMap;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Simple, most-recently-used cache implemented using {@link LinkedHashMap}.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> key type.
 * @param <V> value type.
 */
public class LruMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private final int maxEntries;

    /**
     * <p>
     * Create a new {@link LruMap} with insertion order (i.e. a FIFO).
     * </p>
     * 
     * @param maxEntries The maximum entries to keep, greater zero.
     */
    public static <K, V> LruMap<K, V> insertionOrder(int maxEntries) {
        return new LruMap<K, V>(maxEntries, false);
    }

    /**
     * <p>
     * Create a new {@link LruMap} with access order.
     * </p>
     * 
     * @param maxEntries The maximum entries to keep, greater zero.
     */
    public static <K, V> LruMap<K, V> accessOrder(int maxEntries) {
        return new LruMap<K, V>(maxEntries, true);
    }

    private LruMap(int maxEntries, boolean accessOrder) {
        super(maxEntries + 1, 1.1f, accessOrder);
        Validate.isTrue(maxEntries > 0);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

}

package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.LinkedHashMap;

/**
 * <p>
 * Simple, most-recently-used cache implemented using {@link LinkedHashMap}.
 * </p>
 * NOTE: This class is NOT thread-safe.
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
        return new LruMap<>(maxEntries, false);
    }

    /**
     * <p>
     * Create a new {@link LruMap} with access order.
     * </p>
     * 
     * @param maxEntries The maximum entries to keep, greater zero.
     */
    public static <K, V> LruMap<K, V> accessOrder(int maxEntries) {
        return new LruMap<>(maxEntries, true);
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

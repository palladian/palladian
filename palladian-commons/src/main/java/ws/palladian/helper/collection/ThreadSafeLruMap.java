package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * <p>
 * Simple, thread-safe, least-recently-used cache implemented using {@link LinkedHashMap}.
 * </p>
 *
 * @param <K> key type.
 * @param <V> value type.
 * @author David Urbansky
 */
public class ThreadSafeLruMap<K, V> extends MapDecorator<K,V> {

    /**
     * <p>
     * Create a new {@link ThreadSafeLruMap} with insertion order (i.e. a FIFO).
     * </p>
     *
     * @param maxEntries The maximum entries to keep, greater zero.
     */
    public static <K, V> ThreadSafeLruMap<K, V> insertionOrder(int maxEntries) {
        return new ThreadSafeLruMap<>(maxEntries, false);
    }

    /**
     * <p>
     * Create a new {@link ThreadSafeLruMap} with access order.
     * </p>
     *
     * @param maxEntries The maximum entries to keep, greater zero.
     */
    public static <K, V> ThreadSafeLruMap<K, V> accessOrder(int maxEntries) {
        return new ThreadSafeLruMap<>(maxEntries, true);
    }

    private ThreadSafeLruMap(final int maxEntries, boolean accessOrder) {
        super(Collections.synchronizedMap(new LinkedHashMap<K, V>(maxEntries + 1, 1.1f, accessOrder) {
            private static final long serialVersionUID = 12345L;

            @Override
            protected boolean removeEldestEntry(Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        }));
        Validate.isTrue(maxEntries > 0);
    }
}

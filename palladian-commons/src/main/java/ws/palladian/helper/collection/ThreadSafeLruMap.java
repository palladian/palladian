package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <p>
 * Simple, thread-safe, least-recently-used cache implemented using {@link LinkedHashMap}.
 * </p>
 * 
 * @author David Urbansky
 * 
 * @param <K> key type.
 * @param <V> value type.
 */
public class ThreadSafeLruMap<K, V> implements Map<K,V> {

    private Map<K, V> map = null;

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
        Validate.isTrue(maxEntries > 0);
        map = Collections.synchronizedMap(new LinkedHashMap<K, V>(maxEntries+1, 1.1f, accessOrder) {
            private static final long serialVersionUID = 12345L;

            @Override
            protected boolean removeEldestEntry(Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        });

    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}

package ws.palladian.helper.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * This class simplifies the initialization of {@link Map}s. The {@link #add(Object, Object)} method allows chaining of
 * method calls, so that {@link Map}s can be created in initialized like so:
 * 
 * <pre>
 * Map&lt;String, Integer&gt; map = MapBuilder.createAdd(&quot;key1&quot;, 1).add(&quot;key&quot;, 2);
 * </pre>
 * 
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Type of the key.
 * @param <V> Type of the value.
 */
public final class MapBuilder<K, V> implements Map<K, V> {

    private final Map<K, V> map;

    /**
     * <p>
     * Create a new {@link MapBuilder} initialized with a {@link HashMap}. For more convenience, prefer using the static
     * {@link #createAdd(Object, Object)} factory method.
     * </p>
     */
    public MapBuilder() {
        this.map = new HashMap<K, V>();
    }

    /**
     * <p>
     * Create a new {@link MapBuilder} initialized with the supplied {@link Map}.
     * </p>
     * 
     * @param map
     */
    public MapBuilder(Map<K, V> map) {
        this.map = map;
    }

    /**
     * <p>
     * Create a new {@link MapBuilder} initialized with a {@link HashMap} and add the supplied key-value pair. This is
     * syntactic sugar which saves invoking the constructor, thereby providing type inference.
     * </p>
     * 
     * @param key
     * @param value
     * @return
     */
    public static <K, V> MapBuilder<K, V> createAdd(K key, V value) {
        return new MapBuilder<K, V>().add(key, value);
    }

    /**
     * 
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * @param value
     * @return
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     * @return
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /**
     * @param o
     * @return
     * @see java.util.Map#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public V get(Object key) {
        return map.get(key);
    }

    /**
     * @return
     * @see java.util.Map#hashCode()
     */
    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * @return
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * @return
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * @param key
     * @param value
     * @return
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    /**
     * @param m
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    /**
     * @return
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * @return
     * @see java.util.Map#values()
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    // added MapBuilder methods

    /**
     * <p>
     * Put a new key/value pair to the map, return {@link MapBuilder} to allow method chaining.
     * </p>
     * 
     * @param key
     * @param value
     * @return
     */
    public MapBuilder<K, V> add(K key, V value) {
        put(key, value);
        return this;
    }

    /**
     * <p>
     * Return the wrapped {@link Map}.
     * </p>
     * 
     * @return
     */
    public Map<K, V> getMap() {
        return map;
    }

}

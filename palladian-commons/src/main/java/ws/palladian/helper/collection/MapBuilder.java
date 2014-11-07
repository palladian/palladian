package ws.palladian.helper.collection;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class simplifies the initialization of {@link Map}s. The {@link #add(Object, Object)} method allows chaining of
 * method calls, so that {@link Map}s can be created in initialized like so:
 * 
 * <pre>
 * Map&lt;String, Integer&gt; map = MapBuilder.createAdd(&quot;key1&quot;, 1).add(&quot;key&quot;, 2).create();
 * </pre>
 * 
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Type of the key.
 * @param <V> Type of the value.
 */
public final class MapBuilder<K, V> implements Factory<Map<K, V>> {

    private final Map<K, V> map;

    /**
     * <p>
     * Create a new {@link MapBuilder} initialized with a copy of the supplied {@link Map}.
     * </p>
     * 
     * @param map The map with entries to add to this builder.
     * @return The builder. Use {@link #put(Object, Object)} to add further entries.
     */
    public static <K, V> MapBuilder<K, V> createWith(Map<K, V> map) {
        return new MapBuilder<K, V>(map);
    }

    /**
     * <p>
     * Create a new {@link MapBuilder} initialized with a {@link HashMap}.
     * </p>
     * 
     * @return The builder. Use {@link #put(Object, Object)} to add further entries.
     */
    public static <K, V> MapBuilder<K, V> createEmpty() {
        return new MapBuilder<K, V>();
    }

    /**
     * <p>
     * Create a new {@link MapBuilder} initialized with a {@link HashMap} and add the supplied key-value pair.
     * </p>
     * 
     * @param key The key to add.
     * @param value The value to add.
     * @return The builder. Use {@link #put(Object, Object)} to add further entries.
     */
    public static <K, V> MapBuilder<K, V> createPut(K key, V value) {
        return new MapBuilder<K, V>().put(key, value);
    }

    private MapBuilder() {
        this.map = new HashMap<K, V>();
    }

    private MapBuilder(Map<K, V> map) {
        this.map = new HashMap<K, V>(map);
    }

    /**
     * <p>
     * Put a new key/value pair to the map, return {@link MapBuilder} to allow method chaining.
     * </p>
     * 
     * @param key
     * @param value
     * @return
     */
    public MapBuilder<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    @Override
    public Map<K, V> create() {
        return map;
    }

}

package ws.palladian.helper.collection;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A MultiMap can store multiple values for a key. It is basically a Map with a {@link Collection} assigned to each key.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Type of key.
 * @param <V> Type of values.
 */
public interface MultiMap<K, V> extends Map<K, Collection<V>> {

    /**
     * <p>
     * Add a value to the collection of the specified key.
     * </p>
     * 
     * @param key The key.
     * @param value The value.
     */
    void add(K key, V value);

    /**
     * <p>
     * Add values to the collection of the specified key.
     * </p>
     * 
     * @param key The key.
     * @param values The values.
     */
    void addAll(K key, Collection<? extends V> values);

    /**
     * <p>
     * Add all values from another {@link MultiMap}.
     * </p>
     * 
     * @param multiMap The MultiMap from which to add all values.
     */
    void addAll(MultiMap<? extends K, ? extends V> multiMap);

    /**
     * <p>
     * Get a collections of all single values.
     * </p>
     * 
     * @return Collection with all values.
     */
    Collection<V> allValues();

}

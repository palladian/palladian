package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A MultiMap can store multiple values for a key. It is basically a Map with a List assigned to each key.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Type of key.
 * @param <V> Type of values.
 */
public class MultiMap<K, V> implements Map<K, List<V>> {

    private final Map<K, List<V>> map;

    /**
     * <p>
     * Convenience constructor which allows omitting the redundant type parameters.
     * </p>
     * 
     * @return A new instance of MultiMap.
     */
    public static <K, V> MultiMap<K, V> create() {
        return new MultiMap<K, V>();
    }

    /**
     * @param map
     */
    public MultiMap() {
        this.map = new HashMap<K, List<V>>();
    }

    // java.Util.Map API

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
    public Set<Entry<K, List<V>>> entrySet() {
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
    public List<V> get(Object key) {
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
    public List<V> put(K key, List<V> value) {
        return map.put(key, value);
    }

    /**
     * @param m
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends List<V>> m) {
        map.putAll(m);
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public List<V> remove(Object key) {
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
    public Collection<List<V>> values() {
        return map.values();
    }

    // MultiMap specific API

    public void add(K key, V value) {
        List<V> values = map.get(key);
        if (values == null) {
            values = new ArrayList<V>();
            map.put(key, values);
        }
        values.add(value);
    }

    public List<V> allValues() {
        List<V> values = new ArrayList<V>();
        for (List<V> value : map.values()) {
            values.addAll(value);
        }
        return values;
    }

    /**
     * <p>
     * Get the first value for the given key.
     * </p>
     * 
     * @param key The key for which to retrieve the first value.
     * @return The first value for the given key, or <code>null</code> if no values exist.
     */
    public V getFirst(K key) {
        List<V> values = get(key);
        if (values == null) {
            return null;
        }
        return CollectionHelper.getFirst(values);
    }

}

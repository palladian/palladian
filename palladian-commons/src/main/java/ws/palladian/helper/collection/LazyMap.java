package ws.palladian.helper.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A {@link LazyMap} decorates an ordinary {@link Map}, but creates objects which are not present in the map when they
 * are requested using {@link #get(Object)}. Therefore the LazyMap is initialized with a {@link Factory} closure which
 * takes care of creating the object as necessary.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Key.
 * @param <V> Value.
 */
public final class LazyMap<K, V> implements Map<K, V> {

    private final Map<K, V> map;
    private final Factory<V> factory;

    private LazyMap(Map<K,V> map, Factory<V> factory) {
        this.map = map;
        this.factory = factory;
    }

    public static <K, V> LazyMap<K, V> create(Factory<V> factory) {
        return new LazyMap<K, V>(new HashMap<K, V>(), factory);
    }
    
    public static <K, V> LazyMap<K, V> create(Map<K,V> map, Factory<V> factory) {
        return new LazyMap<K, V>(map, factory);
    }

    /**
     * 
     * @see java.util.Map#clear()
     */
    public void clear() {
        map.clear();
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * @param value
     * @return
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     * @return
     * @see java.util.Map#entrySet()
     */
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /**
     * @param o
     * @return
     * @see java.util.Map#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return map.equals(o);
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#get(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V value = map.get(key);
        if (value == null) {
            value = factory.create();
            map.put((K)key, value);
        }
        return value;
    }

    /**
     * @return
     * @see java.util.Map#hashCode()
     */
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * @return
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * @return
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * @param key
     * @param value
     * @return
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public V put(K key, V value) {
        return map.put(key, value);
    }

    /**
     * @param m
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#remove(java.lang.Object)
     */
    public V remove(Object key) {
        return map.remove(key);
    }

    /**
     * @return
     * @see java.util.Map#size()
     */
    public int size() {
        return map.size();
    }

    /**
     * @return
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
        return map.values();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LazyMap [map=");
        builder.append(map);
        builder.append("]");
        return builder.toString();
    }

}

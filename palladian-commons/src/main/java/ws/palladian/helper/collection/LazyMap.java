package ws.palladian.helper.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ws.palladian.helper.functional.Factory;

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

    @Override
    public void clear() {
        map.clear();
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
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V value = map.get(key);
        if (value == null) {
            value = factory.create();
            map.put((K)key, value);
        }
        return value;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LazyMap [map=");
        builder.append(map);
        builder.append("]");
        return builder.toString();
    }

}

package ws.palladian.helper.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class MapDecorator<K, V> implements Map<K, V> {

    private final Map<K, V> map;

    protected MapDecorator(Map<K, V> map) {
        this.map = Objects.requireNonNull(map, "map must not be null");
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
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * @return The wrapped map.
     */
    public final Map<K, V> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return map.toString();
    }

}

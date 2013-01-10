package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A special {@link Map} where keys are treated case-insensitively.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <V> The type of values in the map.
 */
public class CaseInsensitiveMap<V> implements Map<String, V>, Serializable {

    private static final long serialVersionUID = 1L;

    /** The wrapped map. */
    private final Map<String, V> map;

    /**
     * @param map
     */
    public CaseInsensitiveMap(Map<String, V> map) {
        this.map = new HashMap<String, V>();
        this.map.putAll(map);
    }

    public CaseInsensitiveMap() {
        this.map = new HashMap<String, V>();
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
        return map.containsKey(key.toString().toLowerCase());
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key.toString().toLowerCase());
    }

    @Override
    public V put(String key, V value) {
        return map.put(key.toLowerCase(), value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key.toString().toLowerCase());
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        for (String key : m.keySet()) {
            map.put(key.toLowerCase(), m.get(key));
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CaseInsensitiveMap [map=");
        builder.append(map);
        builder.append("]");
        return builder.toString();
    }

}

package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.*;

/**
 * @author Klemens Muthmann
 */
public final class BidiMap<K, V> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<K, V> map;
    private final Map<V, K> reverseMap;

    /**
     *
     */
    public BidiMap() {
        super();
        map = new HashMap<>();
        reverseMap = new HashMap<>();
    }

    public BidiMap(Map<K, V> map, Map<V, K> reverseMap) {
        super();
        this.map = map;
        this.reverseMap = reverseMap;
    }

    public BidiMap(int initialCapacity) {
        super();
        map = new HashMap<>(initialCapacity);
        reverseMap = new HashMap<>(initialCapacity);
    }

    public BidiMap(int initialCapacity, float loadFactor) {
        super();
        map = new HashMap<>(initialCapacity, loadFactor);
        reverseMap = new HashMap<>(initialCapacity, loadFactor);
    }

    public BidiMap(Map<? extends K, ? extends V> m) {
        super();
        map = new HashMap<>(m);
        reverseMap = new HashMap<>();
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    public void clear() {
        map.clear();
        reverseMap.clear();
    }

    @Override
    public boolean containsKey(Object arg0) {
        return map.containsKey(arg0);
    }

    @Override
    public boolean containsValue(Object arg0) {
        return reverseMap.containsKey(arg0);
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public V get(Object arg0) {
        return map.get(arg0);
    }

    public K getKey(V value) {
        return reverseMap.get(value);
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
        if (map.containsKey(key)) {
            reverseMap.remove(map.get(key));
        }
        if (reverseMap.containsKey(value)) {
            map.remove(reverseMap.get(value));
        }
        V obj = map.put(key, value);
        reverseMap.put(value, key);
        return obj;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> arg0) {
        for (Map.Entry<? extends K, ? extends V> entry : arg0.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        V value = null;
        if (map.containsKey(key)) {
            value = map.remove(key);
            reverseMap.remove(value);
        }
        return value;
    }

    public void removeAll(Collection<K> keys) {
        map.keySet().removeAll(keys);
        // now iterate over the reverse map and remove the entry if it doesn't point to a valid key anymore
        Iterator<V> iterator = reverseMap.keySet().iterator();
        while (iterator.hasNext()) {
            V value = iterator.next();
            if (!map.containsValue(value)) {
                iterator.remove();
            }
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

}

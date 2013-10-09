package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultMultiMap<K, V> implements MultiMap<K, V> {

    private final Map<K, Collection<V>> map;
    private final Factory<Collection<V>> collectionFactory;

    /**
     * <p>
     * Convenience constructor which allows omitting the redundant type parameters.
     * </p>
     * 
     * @return A new instance of MultiMap.
     */
    public static <K, V> MultiMap<K, V> createWithList() {
        return new DefaultMultiMap<K, V>(new Factory<Collection<V>>() {
            @Override
            public Collection<V> create() {
                return CollectionHelper.newArrayList();
            }
        });
    }

    /**
     * <p>
     * Convenience constructor which allows omitting the redundant type parameters.
     * </p>
     * 
     * @return A new instance of MultiMap.
     */
    public static <K, V> MultiMap<K, V> createWithSet() {
        return new DefaultMultiMap<K, V>(new Factory<Collection<V>>() {
            @Override
            public Collection<V> create() {
                return CollectionHelper.newHashSet();
            }
        });
    }

    public DefaultMultiMap(Factory<Collection<V>> collectionFactory) {
        this.map = new HashMap<K, Collection<V>>();
        this.collectionFactory = collectionFactory;
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
    public Set<Entry<K, Collection<V>>> entrySet() {
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
    public Collection<V> get(Object key) {
        Collection<V> value = map.get(key);
        return value != null ? value : Collections.<V> emptySet();
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
    public Collection<V> put(K key, Collection<V> value) {
        return map.put(key, value);
    }

    /**
     * @param m
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends Collection<V>> m) {
        map.putAll(m);
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public Collection<V> remove(Object key) {
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
    public Collection<Collection<V>> values() {
        return map.values();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    // MultiMap specific API

    @Override
    public void add(K key, V value) {
        addAll(key, Collections.singleton(value));
    }

    @Override
    public void addAll(K key, Collection<? extends V> values) {
        Collection<V> existingValues = map.get(key);
        if (existingValues == null) {
            existingValues = collectionFactory.create();
            map.put(key, existingValues);
        }
        existingValues.addAll(values);
    }

    @Override
    public void addAll(MultiMap<? extends K, ? extends V> multiMap) {
        for (K key : multiMap.keySet()) {
            addAll(key, multiMap.get(key));
        }
    }

    @Override
    public List<V> allValues() {
        List<V> values = new ArrayList<V>();
        for (Collection<V> value : map.values()) {
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
        Collection<V> values = get(key);
        if (values == null) {
            return null;
        }
        return CollectionHelper.getFirst(values);
    }

}

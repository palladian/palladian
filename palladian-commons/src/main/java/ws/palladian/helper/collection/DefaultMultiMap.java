package ws.palladian.helper.collection;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * {@link MultiMap} implementation. It provides two default implementations, one using a {@link List} to store values,
 * one using a {@link Set} to store values; see the two static factory methods. If you need a different {@link Collection} type to store values, use the constructor and provide a {@link Factory} to create the desired collection.
 * </p>
 * 
 * @author pk
 * 
 * @param <K> Type of key.
 * @param <V> Type of values.
 */
public class DefaultMultiMap<K, V> extends AbstractMap<K,Collection<V>> implements MultiMap<K, V> {

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

    /**
     * <p>
     * Create a new {@link MultiMap} with a {@link Factory} which takes care of creating the value {@link Collection}s.
     * Usually you either want a {@link Set} or {@link List}, then use the static methods for instantiation instead of
     * the constructor.
     * </p>
     * 
     * @param collectionFactory The factory which creates the {@link Collection}s for the key, not <code>null</code>.
     */
    public DefaultMultiMap(Factory<Collection<V>> collectionFactory) {
        Validate.notNull(collectionFactory, "collectionFactory must not be null");
        this.map = new HashMap<K, Collection<V>>();
        this.collectionFactory = collectionFactory;
    }

    // java.util.Map API

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
    public Set<Entry<K, Collection<V>>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public Collection<V> get(Object key) {
        Collection<V> value = map.get(key);
        return value != null ? value : Collections.<V> emptySet();
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
    public Collection<V> put(K key, Collection<V> value) {
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends Collection<V>> m) {
        map.putAll(m);
    }

    @Override
    public Collection<V> remove(Object key) {
        return map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }

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

    @Override
    public V getFirst(K key) {
        Collection<V> values = get(key);
        return values != null ? CollectionHelper.getFirst(values) : null; 
    }

}

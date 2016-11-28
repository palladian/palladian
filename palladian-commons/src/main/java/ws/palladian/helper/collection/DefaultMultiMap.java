package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Factory;

/**
 * <p>
 * {@link MultiMap} implementation. It provides two default implementations, one
 * using a {@link List} to store values, one using a {@link Set} to store
 * values; see the two static factory methods. If you need a different
 * {@link Collection} type to store values, use the constructor and provide a
 * {@link Factory} to create the desired collection.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K>
 *            Type of key.
 * @param <V>
 *            Type of values.
 */
public class DefaultMultiMap<K, V> extends MapDecorator<K, Collection<V>> implements MultiMap<K, V> {

    private final Factory<? extends Collection<V>> collectionFactory;

    /**
     * <p>
     * Convenience constructor which allows omitting the redundant type parameters.
     * </p>
     * 
     * @return A new instance of MultiMap.
     */
    public static <K, V> MultiMap<K, V> createWithList() {
        return new DefaultMultiMap<>(ArrayList::new);
    }

    /**
     * <p>
     * Convenience constructor which allows omitting the redundant type parameters.
     * </p>
     * 
     * @return A new instance of MultiMap.
     */
    public static <K, V> MultiMap<K, V> createWithSet() {
        return new DefaultMultiMap<>(HashSet::new);
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
    public DefaultMultiMap(Factory<? extends Collection<V>> collectionFactory) {
    	super(new HashMap<>());
        Validate.notNull(collectionFactory, "collectionFactory must not be null");
        this.collectionFactory = collectionFactory;
    }

    // java.util.Map API

    @Override
    public Collection<V> get(Object key) {
        Collection<V> value = getMap().get(key);
        return value != null ? value : Collections.<V> emptySet();
    }

    // MultiMap specific API

    @Override
    public void add(K key, V value) {
        addAll(key, Collections.singleton(value));
    }

    @Override
    public void addAll(K key, Collection<? extends V> values) {
        Collection<V> existingValues = getMap().get(key);
        if (existingValues == null) {
            existingValues = collectionFactory.create();
            put(key, existingValues);
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
        List<V> values = new ArrayList<>();
        for (Collection<V> value : values()) {
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

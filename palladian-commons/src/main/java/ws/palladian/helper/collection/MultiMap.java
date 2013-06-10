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

    void add(K key, V value);

    void addAll(K key, Collection<? extends V> values);

    Collection<V> allValues();

}

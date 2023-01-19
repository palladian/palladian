package ws.palladian.helper.collection;

import ws.palladian.helper.functional.Factory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A {@link LazyMap} decorates an ordinary {@link Map}, but creates objects which are not present in the map when they
 * are requested using {@link #get(Object)}. Therefore the LazyMap is initialized with a {@link Factory} closure which
 * takes care of creating the object as necessary.
 * </p>
 *
 * @param <K> Key.
 * @param <V> Value.
 * @author Philipp Katz
 */
public final class LazyMap<K, V> extends MapDecorator<K, V> {

    private final Factory<? extends V> factory;

    public LazyMap(Map<K, V> map, Factory<? extends V> factory) {
        super(map);
        this.factory = factory;
    }

    public LazyMap(Factory<? extends V> factory) {
        this(new HashMap<>(), factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V value = getMap().get(key);
        if (value == null) {
            value = factory.create();
            put((K) key, value);
        }
        return value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LazyMap [map=");
        builder.append(getMap());
        builder.append("]");
        return builder.toString();
    }
}

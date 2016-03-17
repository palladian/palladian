package ws.palladian.helper.collection;

import java.util.HashMap;
import java.util.Map;

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
public final class LazyMap<K, V> extends MapDecorator<K, V> {

    private final Factory<? extends V> factory;

    public LazyMap(Map<K,V> map, Factory<? extends V> factory) {
        super(map);
        this.factory = factory;
    }
    
    public LazyMap(Factory<? extends V> factory) {
    	this(new HashMap<K, V>(), factory);
    }

    /** @deprecated This was a convenience constructor; starting with Java 1.7, prefer using the real constructor with diamonds. */
    @Deprecated
    public static <K, V> LazyMap<K, V> create(Factory<? extends V> factory) {
        return new LazyMap<K, V>(new HashMap<K, V>(), factory);
    }
    
    /** @deprecated This was a convenience constructor; starting with Java 1.7, prefer using the real constructor with diamonds. */
    @Deprecated
    public static <K, V> LazyMap<K, V> create(Map<K,V> map, Factory<? extends V> factory) {
        return new LazyMap<K, V>(map, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V value = getMap().get(key);
        if (value == null) {
            value = factory.create();
            put((K)key, value);
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

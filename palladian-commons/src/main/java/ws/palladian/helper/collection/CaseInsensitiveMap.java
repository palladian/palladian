package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.AbstractMap;
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
public class CaseInsensitiveMap<V> extends AbstractMap<String, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The wrapped map. */
    private final Map<String, V> map;

    /**
     * <p>
     * Create a new {@link CaseInsensitiveMap} from the given {@link Map}.
     * </p>
     * 
     * @param map The map from which to copy data to the {@link CaseInsensitiveMap}.
     * @return The {@link CaseInsensitiveMap} with the content from the supplied map.
     */
    public static <T> CaseInsensitiveMap<T> from(Map<String, T> map) {
        return new CaseInsensitiveMap<T>(map);
    }

    /**
     * @param map
     */
    public CaseInsensitiveMap(Map<String, V> map) {
        this();
        putAll(map);
    }

    public CaseInsensitiveMap() {
        this.map = new HashMap<String, V>();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key.toString().toLowerCase());
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

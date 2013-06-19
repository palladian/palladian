package ws.palladian.helper.collection;

import java.util.LinkedHashMap;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Simple, most-recently-used cache implemented using {@link LinkedHashMap}.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> key type.
 * @param <V> value type.
 */
public class MruMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private final int maxEntries;

    /**
     * <p>
     * Create a new {@link MruMap} with insertion-order.
     * </p>
     * 
     * @param maxEntries The maximum entries to keep, greater zero.
     */
    public MruMap(int maxEntries) {
        this(maxEntries, false);
    }

    /**
     * @param maxEntries The maximum entries to keep, greater zero.
     * @param accessOrder <code>true</code> for access-order, <code>false</code> for insertion-order.
     */
    public MruMap(int maxEntries, boolean accessOrder) {
        super(16, 0.75f, accessOrder);
        Validate.isTrue(maxEntries > 0);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

}

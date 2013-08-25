package ws.palladian.helper;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * <p>
 * Extremely simple Lru Cache.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class LruCache<S, T> extends LinkedHashMap<S, T> {

    private static final long serialVersionUID = 7858894760560235319L;

    private final int capacity;

    public LruCache(int capacity) {
        super(capacity + 1, 1.1f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Entry<S, T> arg0) {
        return size() > capacity;
    }

}
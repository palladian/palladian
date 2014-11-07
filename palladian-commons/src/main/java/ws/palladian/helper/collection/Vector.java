package ws.palladian.helper.collection;

/**
 * @author pk
 *
 * @param <K>
 * @param <V>
 */
public interface Vector<K, V> {
    
    /**
     * @param k The key to retrieve.
     * @return The value for the key, or <code>null</code> in case no such key exists.
     */
    V get(K k);

}

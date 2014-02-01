package ws.palladian.helper.collection;

import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * @author pk
 * 
 * @param <K>
 * @param <V>
 */
public interface Vector<K, V> extends Iterable<VectorEntry<K, V>> {

    /**
     * An entry within a {@link Vector}. Behaves similar to an entry in a Map, but is realized as dedicated class so
     * that it can be distinguished and it does not provide a setter for the value.
     * 
     * @author pk
     * 
     * @param <K>
     * @param <V>
     */
    public interface VectorEntry<K, V> {

        /**
         * @return The key of this entry.
         */
        K key();

        /**
         * @return The value of this entry.
         */
        V value();

    }

    /**
     * @param k The key to retrieve.
     * @return The value for the key, or <code>null</code> in case no such key exists.
     */
    V get(K k);

    /**
     * @return The size of this vector (i.e. the number of entries).
     */
    int size();

}

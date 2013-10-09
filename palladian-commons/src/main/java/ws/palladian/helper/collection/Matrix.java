package ws.palladian.helper.collection;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public interface Matrix<K, V> {

    public abstract V get(K x, K y);

    public abstract void set(K x, K y, V value);

    public abstract Set<K> getKeysX();

    public abstract Set<K> getKeysY();

    public abstract int sizeY();

    public abstract int sizeX();

    public abstract String asCsv();

    /**
     * <p>
     * Clears the matrix of all existing entries.
     * </p>
     */
    public abstract void clear();
    
    public abstract List<Pair<K, V>> getRow(K y);
    
    public abstract List<Pair<K, V>> getColumn(K x);
 
}
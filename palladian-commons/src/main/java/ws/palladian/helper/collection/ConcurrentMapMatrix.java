package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * A performant map matrix designed for highly concurrent scenarios. Slow column access is not permitted.
 * </p>
 * 
 * @param <K> Type of the keys.
 * @param <V> Type of the values.
 */
public class ConcurrentMapMatrix<K, V> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    /** The maps holding the matrix. */
    private final Map<K, Map<K, V>> matrix = new ConcurrentHashMap<>();

    /** All keys for the y-axis used in the matrix. */
    private final Set<K> keysY = Collections.synchronizedSet(new HashSet<>());

    public static <K, V> ConcurrentMapMatrix<K, V> create() {
        return new ConcurrentMapMatrix<>();
    }

     public Map<K, V> getRow(K y) {
         // return Optional.ofNullable(matrix.get(y)).orElse(new HashMap<K, V>());
    	 return matrix.get(y);
    }

    public void set(K x, K y, V value) {
        synchronized (matrix) {
            Map<K, V> row = matrix.get(y);
            if (row == null) {
                row = new ConcurrentHashMap<>();
                matrix.put(y, row);
            }
            keysY.add(y);
            row.put(x, value);
        }
    }

    public Set<K> getRowKeys() {
        return keysY;
    }

    public void clear() {
        matrix.clear();
        keysY.clear();
    }

    public void removeRow(K y) {
        matrix.remove(y);
        keysY.remove(y);
    }

    public V get(K x, K y) {
        Map<K, V> row = getRow(y);
        return row != null ? row.get(x) : null;
    }
}
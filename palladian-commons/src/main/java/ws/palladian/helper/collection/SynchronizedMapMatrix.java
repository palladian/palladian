package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * A {@link Matrix} which is implemented by using nested {@link Map}s. The outer map holds the rows, the inner maps hold
 * the columns. When using this class in performance critical environments, note that accessing a row using
 * {@link #getRow(Object)} is <b>much</b> faster than accessing a column using {@link #getColumn(Object)}, because in
 * the latter case, all entries need to be iterated.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <K> Type of the keys.
 * @param <V> Type of the values.
 */
public class SynchronizedMapMatrix<K, V> extends AbstractMatrix<K, V> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 2L;

    /** The maps holding the matrix. */
    private final Map<K, Map<K, V>> matrix = Collections.synchronizedMap(new HashMap<K, Map<K, V>>());

    /** All keys for the x-axis used in the matrix. */
    private final Set<K> keysX = Collections.synchronizedSet(new LinkedHashSet<K>());

    /** All keys for the y-axis used in the matrix. */
    private final Set<K> keysY = Collections.synchronizedSet(new LinkedHashSet<K>());

    public static <K, V> SynchronizedMapMatrix<K, V> create() {
        return new SynchronizedMapMatrix<>();
    }

    @Override
    public MatrixVector<K, V> getRow(K y) {
        Map<K, V> row = matrix.get(y);
        return row != null ? new MapMatrixVector<>(y, row) : null;
    }

    @Override
    public MatrixVector<K, V> getColumn(K x) {
        Map<K, V> column = new HashMap<>();
        for (Entry<K, Map<K, V>> row : matrix.entrySet()) {
            K y = row.getKey();
            for (Entry<K, V> cell : row.getValue().entrySet()) {
                if (cell.getKey().equals(x)) {
                    column.put(y, cell.getValue());
                }
            }
        }
        return column.size() > 0 ? new MapMatrixVector<>(x, column) : null;
    }

    @Override
    public void set(K x, K y, V value) {
        Map<K, V> row = matrix.get(y);
        if (row == null) {
            row = Collections.synchronizedMap(new HashMap<K, V>());
            matrix.put(y, row);
        }
        keysX.add(x);
        keysY.add(y);
        row.put(x, value);
    }

    @Override
    public Set<K> getColumnKeys() {
        return keysX;
    }

    @Override
    public Set<K> getRowKeys() {
        return keysY;
    }

    @Override
    public void clear() {
        matrix.clear();
        keysX.clear();
        keysY.clear();
    }

    @Override
    public void removeColumn(K x) {
        for (Map<K, V> row : matrix.values()) {
            row.remove(x);
        }
        keysX.remove(x);
    }

    @Override
    public void removeRow(K y) {
        matrix.remove(y);
        keysY.remove(y);
    }

}
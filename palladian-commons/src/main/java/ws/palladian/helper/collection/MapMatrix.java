package ws.palladian.helper.collection;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * <p>
 * A {@link Matrix} which is implemented by using nested {@link Map}s. The outer map holds the rows, the inner maps hold
 * the columns. When using this class in performance critical environments, note that accessing a row using
 * {@link #getRow(Object)} is <b>much</b> faster than accessing a column using {@link #getColumn(Object)}, because in
 * the latter case, all entries need to be iterated.
 * </p>
 *
 * @param <K> Type of the keys.
 * @param <V> Type of the values.
 * @author Philipp Katz
 */
public class MapMatrix<K, V> extends AbstractMatrix<K, V> implements Serializable {
    /** The serial version id. */
    private static final long serialVersionUID = 2L;

    /** The maps holding the matrix. */
    private final Map<K, Map<K, V>> matrix = new Object2ObjectOpenHashMap<>();

    /** All keys for the x-axis used in the matrix. */
    private final Set<K> keysX = new ObjectLinkedOpenHashSet<>();

    /** All keys for the y-axis used in the matrix. */
    private final Set<K> keysY = new ObjectLinkedOpenHashSet<>();

    @Override
    public MatrixVector<K, V> getRow(K y) {
        Map<K, V> row = matrix.get(y);
        return row != null ? new MapMatrixVector<>(y, row) : null;
    }

    @Override
    public MatrixVector<K, V> getColumn(K x) {
        Map<K, V> column = new Object2ObjectOpenHashMap<>();
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
            row = new HashMap<>();
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
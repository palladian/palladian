package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MapMatrix<K, V> extends AbstractMatrix<K, V> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 2L;

    /** The maps holding the matrix. */
    private final Map<K, Map<K, V>> matrix = CollectionHelper.newHashMap();

    /** All keys for the x-axis used in the matrix. */
    private final Set<K> keysX = CollectionHelper.newLinkedHashSet();

    /** All keys for the y-axis used in the matrix. */
    private final Set<K> keysY = CollectionHelper.newLinkedHashSet();

    @Override
    public Vector<K, V> getRow(K y) {
        Map<K, V> row = matrix.get(y);
        return row != null ? new MapVector<K, V>(row) : null;
    }

    @Override
    public Vector<K, V> getColumn(K x) {
        Map<K, V> column = CollectionHelper.newHashMap();
        for (Entry<K, Map<K, V>> row : matrix.entrySet()) {
            K y = row.getKey();
            for (Entry<K, V> cell : row.getValue().entrySet()) {
                if (cell.getKey().equals(x)) {
                    column.put(y, cell.getValue());
                }
            }
        }
        return column.size() > 0 ? new MapVector<K, V>(column) : null;
    }

    @Override
    public void set(K x, K y, V value) {
        Map<K, V> row = matrix.get(y);
        if (row == null) {
            row = new HashMap<K, V>();
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

}

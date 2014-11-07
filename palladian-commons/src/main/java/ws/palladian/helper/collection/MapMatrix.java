package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapMatrix<K, V> extends AbstractMatrix<K, V> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 8789241892771529365L;

    /** The maps holding the matrix. */
    private final Map<K, Map<K, V>> matrix;

    /** All keys for the x-axis used in the matrix. */
    private final Set<K> keysX;

    /** All keys for the y-axis used in the matrix. */
    private final Set<K> keysY;

    public MapMatrix() {
        matrix = CollectionHelper.newHashMap();
        keysX = CollectionHelper.newLinkedHashSet();
        keysY = CollectionHelper.newLinkedHashSet();
    }

    @Override
    public V get(K x, K y) {
        Map<K, V> column = matrix.get(x);
        if (column == null) {
            return null;
        }
        V item = column.get(y);
        if (item == null) {
            return null;
        }
        return item;
    }

    @Override
    public void set(K x, K y, V value) {
        Map<K, V> column = matrix.get(x);
        if (column == null) {
            column = new HashMap<K, V>();
            matrix.put(x, column);
        }
        keysX.add(x);
        keysY.add(y);
        column.put(y, value);
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

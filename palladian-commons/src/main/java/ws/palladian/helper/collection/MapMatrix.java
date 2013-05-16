package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

public class MapMatrix<K, V> implements Serializable, Matrix<K, V> {

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
        keysX = new TreeSet<K>();
        keysY = new TreeSet<K>();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#get(K, K)
     */
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#set(K, K, V)
     */
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#getKeysX()
     */
    @Override
    public Set<K> getKeysX() {
        return keysX;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#getKeysY()
     */
    @Override
    public Set<K> getKeysY() {
        return keysY;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#sizeY()
     */
    @Override
    public int sizeY() {
        return getKeysY().size();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#sizeX()
     */
    @Override
    public int sizeX() {
        return getKeysX().size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean headWritten = false;

        // iterate through all rows (y)
        for (K yKey : keysY) {

            // write table head
            if (!headWritten) {
                builder.append('\t');
                for (K xKey : keysX) {
                    builder.append(xKey).append('\t');
                }
                builder.append('\n');
                headWritten = true;
            }

            builder.append(yKey).append('\t');

            // iterate through all columns (x)
            for (K xKey : keysX) {
                builder.append(get(xKey, yKey)).append('\t');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#asCsv()
     */
    @Override
    public String asCsv() {
        return toString().replace("\t", ";");
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.collection.Matrix#clear()
     */
    @Override
    public void clear() {
        matrix.clear();
        keysX.clear();
        keysY.clear();
    }

    @Override
    public List<Pair<K, V>> getRow(K y) {
        List<Pair<K, V>> row = CollectionHelper.newArrayList();
        for (K x : keysX) {
            V entry = get(x, y);
            if (entry != null) {
                row.add(Pair.of(x, entry));
            }
        }
        return row;
    }

    @Override
    public List<Pair<K, V>> getColumn(K x) {
        Map<K, V> column = matrix.get(x);
        if (column == null) {
            return Collections.emptyList();
        }
        List<Pair<K, V>> result = CollectionHelper.newArrayList();
        for (Entry<K, V> entry : column.entrySet()) {
            result.add(Pair.of(entry.getKey(), entry.getValue()));
        }
        return result;
    }

}

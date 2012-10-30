package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Matrix<K, V> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 8789241892771529365L;

    /** The maps holding the matrix. */
    private final Map<K, Map<K, V>> matrix;

    /** All keys for the x-axis used in the matrix. */
    private final Set<K> keysX;

    /** All keys for the y-axis used in the matrix. */
    private final Set<K> keysY;

    public Matrix() {
        matrix = CollectionHelper.newHashMap();
        keysX = new TreeSet<K>();
        keysY = new TreeSet<K>();
    }

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

    public Set<K> getKeysX() {
        return keysX;
    }

    public Set<K> getKeysY() {
        return keysY;
    }

    public int sizeY() {
        return getKeysY().size();
    }

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

    public String asCsv() {
        return toString().replace("\t", ";");
    }

    /**
     * <p>
     * Clears the matrix of all existing entries.
     * </p>
     */
    public void clear() {
        matrix.clear();
        keysX.clear();
        keysY.clear();
    }

}

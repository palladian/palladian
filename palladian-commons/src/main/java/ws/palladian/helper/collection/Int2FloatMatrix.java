package ws.palladian.helper.collection;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p>
 * A {@link Matrix} which is implemented by using nested {@link Map}s. The outer map holds the rows, the inner maps hold
 * the columns. When using this class in performance critical environments, note that accessing a row using
 * {@link #getRow(Object)} is <b>much</b> faster than accessing a column using {@link #getColumn(Object)}, because in
 * the latter case, all entries need to be iterated.
 * </p>
 *
 * @author Philipp Katz, David Urbansky
 */
public class Int2FloatMatrix extends AbstractMatrix<Integer, Float> implements Serializable {
    /** The serial version id. */
    private static final long serialVersionUID = 2L;

    /** The maps holding the matrix. */
    private final Map<Integer, Map<Integer, Float>> matrix = new Int2ObjectOpenHashMap<>();

    /** All keys for the x-axis used in the matrix. */
    private final IntLinkedOpenHashSet keysX = new IntLinkedOpenHashSet();

    /** All keys for the y-axis used in the matrix. */
    private final IntLinkedOpenHashSet keysY = new IntLinkedOpenHashSet();

    @Override
    public MatrixVector<Integer, Float> getRow(Integer y) {
        Map<Integer, Float> row = matrix.get(y);
        return row != null ? new MapMatrixVector<>(y, row) : null;
    }

    @Override
    public MatrixVector<Integer, Float> getColumn(Integer x) {
        Map<Integer, Float> column = new Int2FloatOpenHashMap();
        for (Entry<Integer, Map<Integer, Float>> row : matrix.entrySet()) {
            Integer y = row.getKey();
            for (Entry<Integer, Float> cell : row.getValue().entrySet()) {
                if (cell.getKey().equals(x)) {
                    column.put(y, cell.getValue());
                }
            }
        }
        return column.size() > 0 ? new MapMatrixVector<>(x, column) : null;
    }

    @Override
    public void set(Integer x, Integer y, Float value) {
        Map<Integer, Float> row = matrix.computeIfAbsent(y, k -> new Int2FloatOpenHashMap());
        keysX.add(x);
        keysY.add(y);
        row.put(x, value);
    }

    @Override
    public IntLinkedOpenHashSet getColumnKeys() {
        return keysX;
    }

    @Override
    public IntLinkedOpenHashSet getRowKeys() {
        return keysY;
    }

    @Override
    public void clear() {
        matrix.clear();
        keysX.clear();
        keysY.clear();
    }

    @Override
    public void removeColumn(Integer x) {
        for (Map<Integer, Float> row : matrix.values()) {
            row.remove(x);
        }
        keysX.remove(x);
    }

    public void removeColumns(Collection<Integer> xs) {
        for (Map<Integer, Float> row : matrix.values()) {
            row.keySet().removeAll(xs);
        }
        keysX.removeAll(xs);
    }

    @Override
    public void removeRow(Integer y) {
        matrix.remove(y);
        keysY.remove(y);
    }

    public void removeRows(Collection<Integer> ys) {
        matrix.keySet().removeAll(ys);
        keysY.removeAll(ys);
    }

    public String toCsv() {
        return toString("\t");
    }

    @Override
    public String toString() {
        return "Int2FloatMatrix: " + size();
    }
}
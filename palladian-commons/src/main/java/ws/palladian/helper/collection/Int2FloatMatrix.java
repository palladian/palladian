package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * A {@link Matrix} which is implemented by using nested {@link Map}s. The outer map holds the rows, the inner maps hold
 * the columns. When using this class in performance critical environments, note that accessing a row using
 * getRow(Object) is <b>much</b> faster than accessing a column using getColumn(Object), because in
 * the latter case, all entries need to be iterated.
 * </p>
 *
 * @author David Urbansky
 */
public class Int2FloatMatrix implements Serializable {
    /** The serial version id. */
    private static final long serialVersionUID = 2L;

    /** The maps holding the matrix. */
    private final Map<Integer, Map<Integer, Float>> matrix = new ConcurrentHashMap<>(); // XXX what about IntObjectConcurrentMap -> trivago doesn't have it but we could contribute it

    //    private final ConcurrentIntFloatMapBuilder intFloatMapBuilder = ConcurrentIntFloatMapBuilder.newBuilder().withBuckets(8).withDefaultValue(0f).withMode(
    //            ConcurrentIntFloatMapBuilder.MapMode.BUSY_WAITING);

    public Map<Integer, Float> getRow(int y) {
        return matrix.get(y);
    }

    public float get(int x, int y) {
        Map<Integer, Float> row = getRow(y);
        return row != null ? Optional.ofNullable(row.get(x)).orElse(0f) : 0f;
    }

    public void set(int x, int y, float value) {
        Map<Integer, Float> row = matrix.computeIfAbsent(y, k -> new ConcurrentHashMap<>());
        //        IntFloatMap row = matrix.computeIfAbsent(y, k -> intFloatMapBuilder.build());
        row.put(x, value);
    }

    public Set<Integer> getRowKeys() {
        return matrix.keySet();
    }

    public int rowCount() {
        return getRowKeys().size();
    }

    public void clear() {
        matrix.clear();
    }

    public void removeColumn(Integer x) {
        for (Map<Integer, Float> row : matrix.values()) {
            row.remove(x);
        }
    }

    public void removeColumns(Collection<Integer> xs) {
        for (Map<Integer, Float> row : matrix.values()) {
            row.keySet().removeAll(xs);
        }
    }

    public void removeRow(Integer y) {
        matrix.remove(y);
    }

    public void removeRows(Collection<Integer> ys) {
        getRowKeys().removeAll(ys);
    }

    //    public String toCsv() {
    //        return toString("\t");
    //    }

    @Override
    public String toString() {
        return "Int2FloatMatrix: " + getRowKeys().size();
    }
}
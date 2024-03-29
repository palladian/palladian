package ws.palladian.helper.collection;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class PairMatrix<K, V> extends AbstractMatrix<K, V> {

    private final Map<Pair<K, K>, V> matrixMap = new HashMap<>();
    private final Set<K> keysX = new LinkedHashSet<>();
    private final Set<K> keysY = new LinkedHashSet<>();

    @Override
    public V get(K x, K y) {
        return matrixMap.get(Pair.of(x, y));
    }

    @Override
    public void set(K x, K y, V value) {
        matrixMap.put(Pair.of(x, y), value);
        keysX.add(x);
        keysY.add(y);
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
        matrixMap.clear();
        keysX.clear();
        keysY.clear();
    }

    @Override
    public MatrixVector<K, V> getRow(K y) {
        Map<K, V> row = new HashMap<>();
        for (K x : keysX) {
            V entry = matrixMap.get(Pair.of(x, y));
            if (entry != null) {
                row.put(x, entry);
            }
        }
        return row.size() > 0 ? new MapMatrixVector<K, V>(y, row) : null;
    }

    @Override
    public MatrixVector<K, V> getColumn(K x) {
        Map<K, V> column = new HashMap<>();
        for (K y : keysY) {
            V entry = matrixMap.get(Pair.of(x, y));
            if (entry != null) {
                column.put(y, entry);
            }
        }
        return column.size() > 0 ? new MapMatrixVector<K, V>(x, column) : null;
    }

    @Override
    public void removeRow(final K y) {
        keysY.remove(y);
        CollectionHelper.remove(matrixMap.keySet(), new Predicate<Pair<K, K>>() {
            @Override
            public boolean test(Pair<K, K> item) {
                return !item.getRight().equals(y);
            }
        });
    }

    @Override
    public void removeColumn(final K x) {
        keysX.remove(x);
        CollectionHelper.remove(matrixMap.keySet(), new Predicate<Pair<K, K>>() {
            @Override
            public boolean test(Pair<K, K> item) {
                return !item.getLeft().equals(x);
            }
        });
    }

}

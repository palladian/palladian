package ws.palladian.helper.collection;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class PairMatrix<K, V> extends AbstractMatrix<K, V> {

    private final Map<Pair<K, K>, V> matrixMap = CollectionHelper.newHashMap();
    private final Set<K> keysX = CollectionHelper.newLinkedHashSet();
    private final Set<K> keysY = CollectionHelper.newLinkedHashSet();

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
        Map<K, V> row = CollectionHelper.newHashMap();
        for (Entry<Pair<K, K>, V> entry : matrixMap.entrySet()) {
            if (entry.getKey().getRight().equals(y)) {
                row.put(entry.getKey().getLeft(), entry.getValue());
            }
        }
        return row.size() > 0 ? new MapMatrixVector<K, V>(y, row) : null;
    }

    @Override
    public MatrixVector<K, V> getColumn(K x) {
        Map<K, V> column = CollectionHelper.newHashMap();
        for (Entry<Pair<K, K>, V> entry : matrixMap.entrySet()) {
            if (entry.getKey().getLeft().equals(x)) {
                column.put(entry.getKey().getRight(), entry.getValue());
            }
        }
        return column.size() > 0 ? new MapMatrixVector<K, V>(x, column) : null;
    }

    @Override
    public void removeRow(final K y) {
        keysY.remove(y);
        CollectionHelper.remove(matrixMap.keySet(), new Filter<Pair<K, K>>() {
            @Override
            public boolean accept(Pair<K, K> item) {
                return !item.getRight().equals(y);
            }
        });
    }

    @Override
    public void removeColumn(final K x) {
        keysX.remove(x);
        CollectionHelper.remove(matrixMap.keySet(), new Filter<Pair<K, K>>() {
            @Override
            public boolean accept(Pair<K, K> item) {
                return !item.getLeft().equals(x);
            }
        });
    }

}

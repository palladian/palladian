package ws.palladian.helper.collection;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class PairMatrix<K, V> extends AbstractMatrix<K, V> {

    private final Map<Pair<K, K>, V> matrixMap;
    private final Set<K> keysX;
    private final Set<K> keysY;

    public PairMatrix() {
        matrixMap = CollectionHelper.newHashMap();
        keysX = CollectionHelper.newLinkedHashSet();
        keysY = CollectionHelper.newLinkedHashSet();
    }

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

}

package ws.palladian.helper.collection;

import java.util.Iterator;

import ws.palladian.helper.collection.Matrix.MatrixEntry;

public abstract class MatrixEntryDecorator<K, V> implements MatrixEntry<K, V> {

    protected abstract MatrixEntry<K, V> getMatrixEntry();

    @Override
    public final K key() {
        return getMatrixEntry().key();
    }

    @Override
    public final String toString() {
        return getMatrixEntry().toString();
    }

    @Override
    public V get(K k) {
        return getMatrixEntry().get(k);
    }

    @Override
    public final int size() {
        return getMatrixEntry().size();
    }

    @Override
    public final Iterator<VectorEntry<K, V>> iterator() {
        return getMatrixEntry().iterator();
    }

}

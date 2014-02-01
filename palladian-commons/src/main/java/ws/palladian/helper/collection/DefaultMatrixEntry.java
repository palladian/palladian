package ws.palladian.helper.collection;

import ws.palladian.helper.collection.Matrix.MatrixEntry;

class DefaultMatrixEntry<K, V> implements MatrixEntry<K, V> {

    private final Vector<K, V> vector;
    private final K key;

    DefaultMatrixEntry(Vector<K, V> vector, K key) {
        this.vector = vector;
        this.key = key;
    }

    @Override
    public Vector<K, V> vector() {
        return vector;
    }

    @Override
    public K key() {
        return key;
    }

    @Override
    public String toString() {
        return key + ": " + vector;
    }

}

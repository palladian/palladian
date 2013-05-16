package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class TransposedMatrix<K, V> implements Matrix<K, V>, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final Matrix<K, V> matrix;
    
    public static <K, V> TransposedMatrix<K, V> of(Matrix<K ,V> matrix) {
        return new TransposedMatrix<K, V>(matrix);
    }

    /**
     * @param matrix
     */
    private TransposedMatrix(Matrix<K, V> matrix) {
        this.matrix = matrix;
    }

    @Override
    public V get(K x, K y) {
        return matrix.get(y, x);
    }

    @Override
    public void set(K x, K y, V value) {
        matrix.set(y, x, value);
    }

    @Override
    public Set<K> getKeysX() {
        return matrix.getKeysY();
    }

    @Override
    public Set<K> getKeysY() {
        return matrix.getKeysX();
    }

    @Override
    public int sizeY() {
        return matrix.sizeX();
    }

    @Override
    public int sizeX() {
        return matrix.sizeY();
    }

    @Override
    public String asCsv() {
        return matrix.asCsv();
    }

    @Override
    public void clear() {
        matrix.clear();
    }

    @Override
    public List<Pair<K, V>> getRow(K y) {
        return matrix.getColumn(y);
    }

    @Override
    public List<Pair<K, V>> getColumn(K x) {
        return matrix.getRow(x);
    }

}

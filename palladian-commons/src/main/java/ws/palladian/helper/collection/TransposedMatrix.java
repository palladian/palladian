package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Set;

public class TransposedMatrix<K, V> extends AbstractMatrix<K, V> implements Serializable {
    
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
    public Set<K> getColumnKeys() {
        return matrix.getRowKeys();
    }

    @Override
    public Set<K> getRowKeys() {
        return matrix.getColumnKeys();
    }

    @Override
    public void clear() {
        matrix.clear();
    }

    @Override
    public Vector<K, V> getRow(K y) {
        return matrix.getColumn(y);
    }

    @Override
    public Vector<K, V> getColumn(K x) {
        return matrix.getRow(x);
    }

}

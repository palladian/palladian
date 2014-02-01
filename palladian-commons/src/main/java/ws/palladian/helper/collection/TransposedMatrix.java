package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.Validate;

public class TransposedMatrix<K, V> extends MatrixDecorator<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new {@link TransposedMatrix}.
     * 
     * @param matrix The matrix to transpose, not <code>null</code>.
     * @return The transposed matrix.
     */
    public static <K, V> TransposedMatrix<K, V> of(Matrix<K, V> matrix) {
        Validate.notNull(matrix, "matrix must not be null");
        return new TransposedMatrix<K, V>(matrix);
    }

    private TransposedMatrix(Matrix<K, V> matrix) {
        super(matrix);
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
    public Iterable<? extends MatrixEntry<K, V>> rows() {
        return matrix.columns();
    }

    @Override
    public Iterable<? extends MatrixEntry<K, V>> columns() {
        return matrix.rows();
    }

    @Override
    public Vector<K, V> getRow(K y) {
        return matrix.getColumn(y);
    }

    @Override
    public Vector<K, V> getColumn(K x) {
        return matrix.getRow(x);
    }

    @Override
    public void removeRow(K y) {
        matrix.removeColumn(y);
    }

    @Override
    public void removeColumn(K x) {
        matrix.removeRow(x);
    }

}

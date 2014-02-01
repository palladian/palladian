package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * Abstract super class for {@link Matrix} implementations which delegates all functionality to a wrapped {@link Matrix}
 * . This way, only the actually changed functionality must be overridden.
 * 
 * @author pk
 * 
 * @param <K>
 * @param <V>
 */
public abstract class MatrixDecorator<K, V> implements Matrix<K, V>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final Matrix<K, V> matrix;

    protected MatrixDecorator(Matrix<K, V> matrix) {
        Validate.notNull(matrix, "matrix must not be null");
        this.matrix = matrix;
    }

    @Override
    public V get(K x, K y) {
        return matrix.get(x, y);
    }

    @Override
    public void set(K x, K y, V value) {
        matrix.set(x, y, value);
    }

    @Override
    public Set<K> getColumnKeys() {
        return matrix.getColumnKeys();
    }

    @Override
    public Set<K> getRowKeys() {
        return matrix.getRowKeys();
    }

    @Override
    public int columnCount() {
        return matrix.columnCount();
    }

    @Override
    public int rowCount() {
        return matrix.rowCount();
    }

    @Override
    public int size() {
        return matrix.size();
    }

    @Override
    public void clear() {
        matrix.clear();
    }

    @Override
    public MatrixVector<K, V> getRow(K y) {
        return matrix.getRow(y);
    }

    @Override
    public MatrixVector<K, V> getColumn(K x) {
        return matrix.getColumn(x);
    }

    @Override
    public Iterable<? extends MatrixVector<K, V>> rows() {
        return matrix.rows();
    }

    @Override
    public Iterable<? extends MatrixVector<K, V>> columns() {
        return matrix.columns();
    }

    @Override
    public void removeRow(K y) {
        matrix.removeRow(y);
    }

    @Override
    public void removeColumn(K x) {
        matrix.removeColumn(x);
    }

    @Override
    public String toString(String separator) {
        return matrix.toString(separator);
    }

    @Override
    public String toString() {
        return matrix.toString();
    }

    @Override
    public boolean isCompatible(Matrix<K, V> other) {
        return matrix.isCompatible(other);
    }

}

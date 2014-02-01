package ws.palladian.helper.math;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.collection.MapMatrix;
import ws.palladian.helper.collection.Matrix;
import ws.palladian.helper.collection.MatrixDecorator;
import ws.palladian.helper.collection.Vector.VectorEntry;

public class NumericMatrix<K> extends MatrixDecorator<K, Double> implements Serializable {

    private final class NumericEntryConverter implements Function<MatrixVector<K, Double>, NumericMatrixVector<K>> {
        @Override
        public NumericMatrixVector<K> compute(MatrixVector<K, Double> input) {
            return new NumericMatrixVector<K>(input);
        }
    }

    public static final class NumericMatrixVector<K> extends AbstractNumericVector<K> implements
            MatrixVector<K, Double> {

        private final MatrixVector<K, Double> vector;

        public NumericMatrixVector(MatrixVector<K, Double> vector) {
            this.vector = vector;
        }

        @Override
        public Double get(K k) {
            Double value = vector.get(k);
            return value != null ? value : 0;
        }

        @Override
        public Iterator<VectorEntry<K, Double>> iterator() {
            return vector.iterator();
        }

        @Override
        public Set<K> keys() {
            return vector.keys();
        }

        @Override
        public K key() {
            return vector.key();
        }

    }

    private static final long serialVersionUID = 1L;

    public NumericMatrix() {
        this(new MapMatrix<K, Double>());
    }

    public NumericMatrix(Matrix<K, Double> matrix) {
        super(matrix);
    }

    /**
     * <p>
     * Add two matrixes.
     * </p>
     * 
     * @param other The matrix to add to the current matrix. The matrix must have the same column and row names as the
     *            matrix it is added to, not <code>null</code>.
     * @return A new matrix, containing the addition.
     */
    public NumericMatrix<K> add(NumericMatrix<K> other) {
        Validate.notNull(other, "other must not be null");
        Validate.isTrue(isCompatible(other), "matrices must be compatible");

        NumericMatrix<K> result = new NumericMatrix<K>();
        for (K yKey : getRowKeys()) {
            NumericVector<K> thisRow = this.getRow(yKey);
            NumericVector<K> otherRow = other.getRow(yKey);
            for (K xKey : getColumnKeys()) {
                double thisValue = thisRow.get(xKey);
                double otherValue = otherRow.get(xKey);
                result.set(xKey, yKey, thisValue + otherValue);
            }
        }
        return result;
    }

    /**
     * <p>
     * Do a scalar multiplication.
     * </p>
     * 
     * @param lambda Value of the scalar.
     * @return A new matrix, representing the scalar multiplication with the given value.
     */
    public NumericMatrix<K> scalar(double lambda) {
        NumericMatrix<K> result = new NumericMatrix<K>();
        for (NumericMatrixVector<K> row : rows()) {
            for (VectorEntry<K, Double> entry : row) {
                result.set(entry.key(), row.key(), entry.value() * lambda);
            }
        }
        return result;
    }

    @Override
    public Double get(K x, K y) {
        Double value = matrix.get(x, y);
        return value != null ? value : 0;
    };

    @Override
    public NumericMatrixVector<K> getRow(K y) {
        return new NumericMatrixVector<K>(matrix.getRow(y));
    }

    @Override
    public NumericMatrixVector<K> getColumn(K x) {
        return new NumericMatrixVector<K>(matrix.getColumn(x));
    }

    @Override
    public Iterable<NumericMatrixVector<K>> rows() {
        return CollectionHelper.convert(matrix.rows(), new NumericEntryConverter());
    }

    @Override
    public Iterable<NumericMatrixVector<K>> columns() {
        return CollectionHelper.convert(matrix.columns(), new NumericEntryConverter());
    }

}

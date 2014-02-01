package ws.palladian.helper.math;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.collection.MapMatrix;
import ws.palladian.helper.collection.Matrix;
import ws.palladian.helper.collection.MatrixDecorator;
import ws.palladian.helper.collection.MatrixEntryDecorator;
import ws.palladian.helper.collection.Vector.VectorEntry;

public class NumericMatrix<K> extends MatrixDecorator<K, Double> implements Serializable {

    private final class NumericEntryConverter implements Function<MatrixEntry<K, Double>, NumericMatrixEntry<K>> {
        @Override
        public NumericMatrixEntry<K> compute(MatrixEntry<K, Double> input) {
            return new NumericMatrixEntry<K>(input);
        }
    }

    public static class NumericMatrixEntry<K> extends MatrixEntryDecorator<K, Double> implements NumericVector<K> {

        private final NumericVector<K> vector;
        private final MatrixEntry<K, Double> matrixEntry;

        public NumericMatrixEntry(MatrixEntry<K, Double> matrixEntry) {
            this.matrixEntry = matrixEntry;
            this.vector = new ImmutableNumericVector<K>(matrixEntry);
        }

        @Override
        public NumericVector<K> add(NumericVector<K> other) {
            return vector.add(other);
        }

        @Override
        public double norm() {
            return vector.norm();
        }

        @Override
        public double dot(NumericVector<K> other) {
            return vector.dot(other);
        }

        @Override
        public double sum() {
            return vector.sum();
        }

        @Override
        public double cosine(NumericVector<K> other) {
            return vector.cosine(other);
        }

        @Override
        public double euclidean(NumericVector<K> other) {
            return vector.euclidean(other);
        }

        @Override
        public Set<K> keys() {
            return vector.keys();
        }
        
        @Override
        public Double get(K k) {
            Double value = matrixEntry.get(k);
            return value != null ? value : 0;
        }

        @Override
        protected MatrixEntry<K, Double> getMatrixEntry() {
            return matrixEntry;
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
        for (NumericMatrixEntry<K> row : rows()) {
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
    public NumericMatrixEntry<K> getRow(K y) {
        return new NumericMatrixEntry<K>(matrix.getRow(y));
    }

    @Override
    public NumericMatrixEntry<K> getColumn(K x) {
        return new NumericMatrixEntry<K>(matrix.getColumn(x));
    }

    @Override
    public Iterable<NumericMatrixEntry<K>> rows() {
        return CollectionHelper.convert(matrix.rows(), new NumericEntryConverter());
    }

    @Override
    public Iterable<NumericMatrixEntry<K>> columns() {
        return CollectionHelper.convert(matrix.columns(), new NumericEntryConverter());
    }

}

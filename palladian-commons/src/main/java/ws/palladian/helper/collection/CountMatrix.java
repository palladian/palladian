package ws.palladian.helper.collection;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * A CountMatrix allows counting of items which are indexed by (x, y) coordinates. It is the two-dimensional variant of
 * the {@link CountMap}.
 * </p>
 * 
 * @param <K> The type of the keys in this CountMatrix.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class CountMatrix<K> extends MatrixDecorator<K, Integer> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    private final class EntryConverter implements Function<MatrixEntry<K, Integer>, IntegerMatrixEntry<K>> {
        @Override
        public IntegerMatrixEntry<K> compute(MatrixEntry<K, Integer> input) {
            return new IntegerMatrixEntry<K>(input);
        }
    }

    /**
     * A {@link MatrixEntry} decorator, which returns {@link IntegerVector}s.
     * 
     * @author pk
     * 
     * @param <K>
     */
    public static class IntegerMatrixEntry<K> extends MatrixEntryDecorator<K, Integer> {

        private final MatrixEntry<K, Integer> matrixEntry;
        final int sum;

        public IntegerMatrixEntry(MatrixEntry<K, Integer> matrixEntry) {
            this.matrixEntry = matrixEntry;
            int sum = 0;
            for (VectorEntry<K, Integer> entry : matrixEntry) {
                sum += entry.value();
            }
            this.sum = sum;
        }

        @Override
        protected MatrixEntry<K, Integer> getMatrixEntry() {
            return matrixEntry;
        }

        /**
         * @return The sum of all values in this {@link Vector}.
         */
        public int getSum() {
            return sum;
        }

    }

    public CountMatrix(Matrix<K, Integer> matrix) {
        super(matrix);
    }

    /**
     * <p>
     * Shortcut method instead of constructor which allows omitting the type parameter.
     * </p>
     * 
     * @return A new CountMatrix.
     */
    public static <T> CountMatrix<T> create() {
        return new CountMatrix<T>(new MapMatrix<T, Integer>());
    }

    /**
     * <p>
     * Increment the count of the specified cell by one.
     * </p>
     * 
     * @param x The column, not <code>null</code>.
     * @param y The row, not <code>null</code>.
     */
    public void add(K x, K y) {
        Validate.notNull(x, "x must not be null");
        Validate.notNull(y, "y must not be null");
        add(x, y, 1);
    }

    /**
     * <p>
     * Increment the count of the specified cell by a certain number.
     * </p>
     * 
     * @param x The column, not <code>null</code>.
     * @param y The row, not <code>null</code>.
     * @param value The value to add.
     */
    public void add(K x, K y, int value) {
        Validate.notNull(x, "x must not be null");
        Validate.notNull(y, "y must not be null");
        Integer count = get(x, y);
        set(x, y, count += value);
    }

    /**
     * <p>
     * Same as {@link #get(Object, Object)}, just to be consistent to CountMap's method.
     * </p>
     * 
     * @param x
     * @param y
     * @return
     */
    public int getCount(K x, K y) {
        return get(x, y);
    }

    @Override
    public Integer get(K x, K y) {
        Integer result = matrix.get(x, y);
        return result != null ? result : 0;
    }

    @Override
    public Iterable<IntegerMatrixEntry<K>> rows() {
        return CollectionHelper.convert(matrix.rows(), new EntryConverter());
    }

    @Override
    public Iterable<IntegerMatrixEntry<K>> columns() {
        return CollectionHelper.convert(matrix.columns(), new EntryConverter());
    }

    /**
     * <p>
     * In contrast to what is stated in the interface, this methods returns an empty number vector, in case the
     * specified row does not exist for your convenience.
     * </p>
     */
    @Override
    public IntegerMatrixEntry<K> getRow(K y) {
        Validate.notNull(y, "y must not be null");
        MatrixEntry<K, Integer> row = matrix.getRow(y);
        return new IntegerMatrixEntry<K>(row != null ? row : new NullMatrixEntry<K, Integer>(y));
    }

    /**
     * <p>
     * In contrast to what is stated in the interface, this methods returns an empty number vector, in case the
     * specified column does not exist for your convenience.
     * </p>
     */
    @Override
    public IntegerMatrixEntry<K> getColumn(K x) {
        Validate.notNull(x, "x must not be null");
        MatrixEntry<K, Integer> column = matrix.getColumn(x);
        return new IntegerMatrixEntry<K>(column != null ? column : new NullMatrixEntry<K, Integer>(x));
    }

    public int getSum() {
        int totalSize = 0;
        for (K y : getRowKeys()) {
            totalSize += getRow(y).getSum();
        }
        return totalSize;
    }

}
